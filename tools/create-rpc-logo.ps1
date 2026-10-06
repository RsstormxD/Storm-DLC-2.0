$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$source = Join-Path $taskRoot 'src/main/resources/assets/stormdlc/logo.png'
$target = Join-Path $taskRoot 'src/main/resources/assets/stormdlc/discord-logo-spinning.gif'
# A finite input is required so palettegen can finish before paletteuse starts.
& ffmpeg -hide_banner -loglevel error -y -loop 1 -framerate 20 -t 3 -i $source `
    -filter_complex '[0:v]scale=512:512:flags=lanczos,rotate=2*PI*t/3:fillcolor=black,split[frames][palette];[palette]palettegen=stats_mode=full[p];[frames][p]paletteuse=dither=sierra2_4a' `
    -t 3 -loop 0 $target
if ($LASTEXITCODE -ne 0) { throw 'RPC logo animation could not be generated.' }
