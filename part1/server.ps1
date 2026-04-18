# Part 1：单机拍卖（与 server.sh 顺序一致，供 Windows PowerShell 使用）
# 用法：在 part1 目录执行  .\server.ps1
# 需已安装 JDK 17+、Maven，且 java/mvn 在 PATH 中

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
Set-Location $Root

$PID_DIR = Join-Path $Root '.pids'
New-Item -ItemType Directory -Force -Path $PID_DIR | Out-Null
$LOG_DIR = Join-Path $Root '.logs'
New-Item -ItemType Directory -Force -Path $LOG_DIR | Out-Null

function New-LogFile($name, $suffix = 'log') {
    $path = Join-Path $LOG_DIR "$name.$suffix"
    for ($i = 0; $i -lt 20; $i++) {
        try {
            $fs = [System.IO.File]::Open($path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write, [System.IO.FileShare]::ReadWrite)
            $fs.Close()
            return $path
        } catch {
            Start-Sleep -Milliseconds 150
        }
    }
    throw "Cannot create log file because it is still locked: $path"
}

function Write-Pid($name, $id) {
    Set-Content -Path (Join-Path $PID_DIR "$name.pid") -Value $id -Encoding ascii
}

function Ensure-Alive($proc, $name, $outLog, $errLog) {
    Start-Sleep -Milliseconds 600
    if ($proc.HasExited) {
        throw "$name exited early (code=$($proc.ExitCode)). Check logs: $outLog and $errLog"
    }
}

function Find-Rmiregistry {
    if ($env:JAVA_HOME) {
        $c = Join-Path $env:JAVA_HOME 'bin\rmiregistry.exe'
        if (Test-Path $c) { return $c }
    }
    $java = (Get-Command java -ErrorAction Stop).Source
    $bin = Split-Path $java
    $c = Join-Path $bin 'rmiregistry.exe'
    if (Test-Path $c) { return $c }
    throw '找不到 rmiregistry.exe，请设置 JAVA_HOME 或将 JDK bin 加入 PATH'
}

function Find-Mvn {
    (Get-Command mvn -ErrorAction Stop).Source
}

Write-Host 'Stopping old processes (best-effort)...'
Get-ChildItem (Join-Path $PID_DIR '*.pid') -ErrorAction SilentlyContinue | ForEach-Object {
    $line = Get-Content -LiteralPath $_.FullName -TotalCount 1 -ErrorAction SilentlyContinue
    if ($null -ne $line) {
        $line = $line.Trim()
        $procId = 0
        if ([int]::TryParse($line, [ref]$procId)) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
            } catch { }
        }
    }
    Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
}
Stop-Process -Name rmiregistry -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 1

Write-Host 'Building...'
& mvn -q clean package
if ($LASTEXITCODE -ne 0) { throw "mvn build failed (exit $LASTEXITCODE)" }

$mvn = Find-Mvn
$rmi = Find-Rmiregistry
$cp = 'target/classes'

Write-Host 'Starting rmiregistry on 1099...'
$regOut = New-LogFile 'rmiregistry' 'out.log'
$regErr = New-LogFile 'rmiregistry' 'err.log'
$reg = Start-Process -FilePath $rmi -ArgumentList @('1099', "-J-Djava.class.path=$cp") `
    -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $regOut -RedirectStandardError $regErr -PassThru
Write-Pid 'rmiregistry' $reg.Id
Ensure-Alive $reg 'rmiregistry' $regOut $regErr
Start-Sleep -Seconds 1

Write-Host 'Starting AuctionServer...'
$srvOut = New-LogFile 'server' 'out.log'
$srvErr = New-LogFile 'server' 'err.log'
$srv = Start-Process -FilePath $mvn -ArgumentList @(
    '-q', 'exec:java', '-Dexec.mainClass=server.ServerMain'
) -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $srvOut -RedirectStandardError $srvErr -PassThru
Write-Pid 'server' $srv.Id
Ensure-Alive $srv 'AuctionServer' $srvOut $srvErr
Start-Sleep -Seconds 1

Write-Host 'Starting FrontEnd (gRPC 50055)...'
$feOut = New-LogFile 'frontend' 'out.log'
$feErr = New-LogFile 'frontend' 'err.log'
$fe = Start-Process -FilePath $mvn -ArgumentList @(
    '-q', 'exec:java', '-Dexec.mainClass=frontend.FrontEndServer'
) -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $feOut -RedirectStandardError $feErr -PassThru
Write-Pid 'frontend' $fe.Id
Ensure-Alive $fe 'FrontEnd' $feOut $feErr

Write-Host ''
Write-Host 'Part 1 ready. New terminal in this folder, then run:'
Write-Host '  .\client.ps1          (PowerShell)'
Write-Host 'Or: mvn exec:java "-Dexec.mainClass=client.AuctionClient"   (quotes required in PowerShell)'
Write-Host "PID files: $PID_DIR"
Write-Host "Log files: $LOG_DIR"
