# 仓库根目录：选择启动 Part 1 或 Part 2（不要同时运行）
# 用法:  .\start.ps1 -Part 1    或    .\start.ps1 -Part 2

param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('1', '2')]
    [string] $Part
)

$Root = $PSScriptRoot
if ($Part -eq '1') {
    & (Join-Path $Root 'part1\server.ps1')
} else {
    & (Join-Path $Root 'part2\server.ps1')
}
