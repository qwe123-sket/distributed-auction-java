# Part 2：复制拍卖（与 server.sh 一致，供 Windows PowerShell 使用）
# 用法：在 part2 目录执行  .\server.ps1
# 不要与 Part 1 同时运行（同一 RMI 端口 1099、gRPC 50055）

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
    Start-Sleep -Milliseconds 700
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

Write-Host 'Starting FrontEnd...'
$feOut = New-LogFile 'frontend' 'out.log'
$feErr = New-LogFile 'frontend' 'err.log'
$fe = Start-Process -FilePath $mvn -ArgumentList @('-q', 'exec:java', '-Dexec.mainClass=frontend.FrontEndServer') `
    -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $feOut -RedirectStandardError $feErr -PassThru
Write-Pid 'frontend' $fe.Id
Ensure-Alive $fe 'FrontEnd' $feOut $feErr
Start-Sleep -Seconds 2

Write-Host 'Starting 3 replicas...'
foreach ($i in 1, 2, 3) {
    $repOut = New-LogFile "replica$i" 'out.log'
    $repErr = New-LogFile "replica$i" 'err.log'
    $p = Start-Process -FilePath $mvn -ArgumentList @(
        '-q', 'exec:java',
        '-Dexec.mainClass=replica.ReplicaMain',
        "-Dexec.args=$i"
    ) -WorkingDirectory $Root -WindowStyle Hidden -RedirectStandardOutput $repOut -RedirectStandardError $repErr -PassThru
    Write-Pid "replica$i" $p.Id
    Ensure-Alive $p "replica$i" $repOut $repErr
    Start-Sleep -Seconds 1
}

Write-Host ''
Write-Host 'Part 2 ready. New terminal in this folder, then run:'
Write-Host '  .\client.ps1          (PowerShell)'
Write-Host 'Or: mvn exec:java "-Dexec.mainClass=client.AuctionClient"   (quotes required in PowerShell)'
Write-Host "PID files: $PID_DIR"
Write-Host "Log files: $LOG_DIR"
