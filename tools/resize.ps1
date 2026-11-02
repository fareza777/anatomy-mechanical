param(
    [Parameter(Mandatory = $true)][string]$In,
    [Parameter(Mandatory = $true)][string]$Out,
    [int]$W = 720,
    [int]$H = 1600
)

Add-Type -AssemblyName System.Drawing
$src = [System.Drawing.Image]::FromFile($In)
$bmp = New-Object System.Drawing.Bitmap $W, $H
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = 'HighQualityBicubic'
$g.DrawImage($src, 0, 0, $W, $H)
$bmp.Save($Out, [System.Drawing.Imaging.ImageFormat]::Jpeg)
$g.Dispose()
$bmp.Dispose()
$src.Dispose()
