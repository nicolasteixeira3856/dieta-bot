<#
.SYNOPSIS
  Builds the launcher icon layers and the splash logo from design/brand/ (A14).

.DESCRIPTION
  design/brand/icon.png       -> mipmap-*/ic_launcher_foreground.png (symbol scaled to 62/108 dp, inside the 66 dp safe zone)
  design/brand/icon-mono.png  -> mipmap-*/ic_launcher_monochrome.png (Android 13+ themed icon)
  design/brand/icon.png       -> drawable-nodpi/logo_mark.png (tight crop, 480 px) for the Compose splash
  The background layer is a color (@color/ic_launcher_bg), not an image.
  Re-run after replacing the files in design/brand/.

.EXAMPLE
  ./tools/brand-icons.ps1
#>
param(
    [string]$Brand = (Join-Path (Split-Path -Parent $PSScriptRoot) "design/brand"),
    [string]$Res = (Join-Path (Split-Path -Parent $PSScriptRoot) "apps/android/app/src/main/res"),
    # Symbol side as a fraction of the 108 dp canvas: 62 dp, inside the 66 dp safe zone, so the husk tip
    # of the tilted oat seed keeps ~3 dp inside the circle mask (D13).
    [double]$SafeZone = 62.0 / 108.0
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$densities = [ordered]@{ "mdpi" = 108; "hdpi" = 162; "xhdpi" = 216; "xxhdpi" = 324; "xxxhdpi" = 432 }

function Get-SymbolBounds([System.Drawing.Bitmap]$bmp) {
    $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
    $data = $bmp.LockBits($rect, [System.Drawing.Imaging.ImageLockMode]::ReadOnly, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    try {
        $bytes = New-Object byte[] ($data.Stride * $bmp.Height)
        [System.Runtime.InteropServices.Marshal]::Copy($data.Scan0, $bytes, 0, $bytes.Length)
    } finally { $bmp.UnlockBits($data) }
    $minX = $bmp.Width; $minY = $bmp.Height; $maxX = -1; $maxY = -1
    for ($y = 0; $y -lt $bmp.Height; $y++) {
        $row = $y * $data.Stride
        for ($x = 0; $x -lt $bmp.Width; $x++) {
            if ($bytes[$row + $x * 4 + 3] -gt 16) {
                if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }
                if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y }
            }
        }
    }
    if ($maxX -lt 0) { throw "no visible pixels" }
    # Square crop around the symbol center so it is never stretched.
    $side = [Math]::Max($maxX - $minX + 1, $maxY - $minY + 1)
    $cx = ($minX + $maxX) / 2.0; $cy = ($minY + $maxY) / 2.0
    return New-Object System.Drawing.RectangleF(($cx - $side / 2.0), ($cy - $side / 2.0), $side, $side)
}

function Save-Scaled([System.Drawing.Bitmap]$src, [System.Drawing.RectangleF]$crop, [int]$canvas, [int]$symbol, [string]$path) {
    $out = New-Object System.Drawing.Bitmap($canvas, $canvas, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($out)
    try {
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $offset = ($canvas - $symbol) / 2.0
        $dest = New-Object System.Drawing.RectangleF($offset, $offset, $symbol, $symbol)
        $g.DrawImage($src, $dest, $crop, [System.Drawing.GraphicsUnit]::Pixel)
    } finally { $g.Dispose() }
    New-Item -ItemType Directory -Force (Split-Path -Parent $path) | Out-Null
    $out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $out.Dispose()
    Write-Host ("  {0} ({1}px, symbol {2}px)" -f $path.Replace((Split-Path -Parent $PSScriptRoot) + [IO.Path]::DirectorySeparatorChar, ""), $canvas, $symbol)
}

foreach ($layer in @(@("icon.png", "ic_launcher_foreground.png"), @("icon-mono.png", "ic_launcher_monochrome.png"))) {
    $srcPath = Join-Path $Brand $layer[0]
    if (-not (Test-Path $srcPath)) { throw "missing $srcPath" }
    $src = New-Object System.Drawing.Bitmap($srcPath)
    try {
        $crop = Get-SymbolBounds $src
        foreach ($d in $densities.GetEnumerator()) {
            $symbol = [int][Math]::Round($d.Value * $SafeZone)
            Save-Scaled $src $crop $d.Value $symbol (Join-Path $Res "mipmap-$($d.Key)/$($layer[1])")
        }
        if ($layer[0] -eq "icon.png") {
            Save-Scaled $src $crop 480 480 (Join-Path $Res "drawable-nodpi/logo_mark.png")
        }
    } finally { $src.Dispose() }
}
