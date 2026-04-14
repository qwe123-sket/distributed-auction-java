# Start Part 1 or Part 2 on Windows (do not run both at once — ports 1099 / 50055).
# Usage:  .\start.ps1 -Part 1   or   .\start.ps1 -Part 2

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
