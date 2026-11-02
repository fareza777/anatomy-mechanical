param(
    [int]$Width = 520,
    [int]$Height = 980,
    [string]$TitleLike = "kubloom"
)

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class WinSize {
    [DllImport("user32.dll")]
    public static extern bool SetWindowPos(IntPtr hWnd, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@

$proc = Get-Process | Where-Object { $_.MainWindowTitle -like "*$TitleLike*" } | Select-Object -First 1
if (-not $proc) { Write-Error "Emulator window not found"; exit 1 }

[WinSize]::SetWindowPos($proc.MainWindowHandle, [IntPtr]::Zero, 70, 20, $Width, $Height, 0x0040) | Out-Null
Write-Output "Resized window to ${Width}x${Height}"
