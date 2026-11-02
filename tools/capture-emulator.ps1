param(
    [string]$Out = "docs/screenshots/emulator-window.png",
    [string]$TitleLike = "kubloom"
)

Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WinCapture {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left, Top, Right, Bottom; }

    [StructLayout(LayoutKind.Sequential)]
    public struct POINT { public int X, Y; }

    [DllImport("user32.dll")]
    public static extern bool GetClientRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool ClientToScreen(IntPtr hWnd, ref POINT point);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    public static void Focus(IntPtr hWnd) {
        ShowWindow(hWnd, 9);
        SetForegroundWindow(hWnd);
    }
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like "*$TitleLike*" } | Select-Object -First 1
if (-not $proc) { Write-Error "Emulator window not found"; exit 1 }

$handle = $proc.MainWindowHandle
[WinCapture]::Focus($handle)
Start-Sleep -Milliseconds 700

$rect = New-Object WinCapture+RECT
if (-not [WinCapture]::GetClientRect($handle, [ref]$rect)) { Write-Error "GetClientRect failed"; exit 1 }
$point = New-Object WinCapture+POINT
[WinCapture]::ClientToScreen($handle, [ref]$point)

$width = $rect.Right - $rect.Left
$height = $rect.Bottom - $rect.Top
if ($width -le 0 -or $height -le 0) { Write-Error "Invalid client rect"; exit 1 }

$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.CopyFromScreen($point.X, $point.Y, 0, 0, $bmp.Size)
$dir = Split-Path -Parent $Out
if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
$bmp.Save((Join-Path (Get-Location) $Out), [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()
$bmp.Dispose()
Write-Output "Captured ${width}x${height} to $Out"
