# Part 2：复制拍卖（与 server.sh 一致，供 Windows PowerShell 使用）
# 用法：在 part2 目录执行  .\server.ps1
# 不要与 Part 1 同时运行（同一 RMI 端口 1099、gRPC 50055）

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
Set-Location $Root

$PID_DIR = Join-Path $Root '.pids'
New-Item -ItemType Directory -Force -Path $PID_DIR | Out-Null

function Write-Pid($name, $id) {
    Set-Content -Path (Join-Path $PID_DIR "$name.pid") -Value $id -Encoding ascii
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
$reg = Start-Process -FilePath $rmi -ArgumentList @('1099', "-J-Djava.class.path=$cp") `
    -WorkingDirectory $Root -WindowStyle Hidden -PassThru
Write-Pid 'rmiregistry' $reg.Id
Start-Sleep -Seconds 1

Write-Host 'Starting FrontEnd...'
$fe = Start-Process -FilePath $mvn -ArgumentList @('-q', 'exec:java', '-Dexec.mainClass=frontend.FrontEndServer') `
    -WorkingDirectory $Root -WindowStyle Hidden -PassThru
Write-Pid 'frontend' $fe.Id
Start-Sleep -Seconds 2

Write-Host 'Starting 3 replicas...'
foreach ($i in 1, 2, 3) {
    $p = Start-Process -FilePath $mvn -ArgumentList @(
        '-q', 'exec:java',
        '-Dexec.mainClass=replica.ReplicaMain',
        "-Dexec.args=$i"
    ) -WorkingDirectory $Root -WindowStyle Hidden -PassThru
    Write-Pid "replica$i" $p.Id
    Start-Sleep -Seconds 1
}

Write-Host ''
Write-Host 'Part 2 ready. New terminal in this folder, then run:'
Write-Host '  .\client.ps1          (PowerShell)'
Write-Host 'Or: mvn exec:java "-Dexec.mainClass=client.AuctionClient"   (quotes required in PowerShell)'
Write-Host "PID files: $PID_DIR"
