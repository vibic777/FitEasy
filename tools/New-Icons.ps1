$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Drawing
$iconRoot=Join-Path $PSScriptRoot '../desktopApp/src/jvmMain/resources/icons'
New-Item -ItemType Directory -Path $iconRoot -Force | Out-Null
$imageBuffers=@()
foreach($iconSize in @(16,32,48,64,128,256)) {
    $bitmap=[System.Drawing.Bitmap]::new($iconSize,$iconSize)
    $graphics=[System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode=[System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.ScaleTransform($iconSize/108.0,$iconSize/108.0)
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $outer=[System.Drawing.Drawing2D.GraphicsPath]::new()
    $outer.AddBezier(54,98,50,98,12,73,9,49)
    $outer.AddBezier(9,49,4,31,15,15,32,14)
    $outer.AddBezier(32,14,43,14,50,20,54,27)
    $outer.AddBezier(54,27,58,20,65,14,76,14)
    $outer.AddBezier(76,14,94,14,104,28,100,47)
    $outer.AddBezier(100,47,97,72,58,98,54,98)
    $outer.CloseFigure()
    $white=[System.Drawing.SolidBrush]::new([System.Drawing.Color]::White)
    $edge=[System.Drawing.Pen]::new([System.Drawing.ColorTranslator]::FromHtml('#D9E5DF'),1.2)
    $edge.LineJoin=[System.Drawing.Drawing2D.LineJoin]::Round
    $graphics.FillPath($white,$outer)
    $graphics.DrawPath($edge,$outer)
    $heart=[System.Drawing.Drawing2D.GraphicsPath]::new()
    $heart.AddBezier(54,85,23,66,19,47,29,36)
    $heart.AddBezier(29,36,38,26,49,30,54,38)
    $heart.AddBezier(54,38,59,30,71,26,80,36)
    $heart.AddBezier(80,36,91,49,83,66,54,85)
    $heart.CloseFigure()
    $green=[System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#337C69'))
    $graphics.FillPath($green,$heart)
    $leaf=[System.Drawing.Drawing2D.GraphicsPath]::new()
    $leaf.AddBezier(54,35,53,18,66,15,80,18)
    $leaf.AddBezier(80,18,80,30,70,39,54,35)
    $leaf.CloseFigure()
    $gold=[System.Drawing.SolidBrush]::new([System.Drawing.ColorTranslator]::FromHtml('#F4CF85'))
    $graphics.FillPath($gold,$leaf)
    $pen=[System.Drawing.Pen]::new([System.Drawing.Color]::White,4)
    $pen.StartCap=$pen.EndCap=[System.Drawing.Drawing2D.LineCap]::Round
    $points=[System.Drawing.PointF[]]@([System.Drawing.PointF]::new(35,54),[System.Drawing.PointF]::new(47,54),[System.Drawing.PointF]::new(53,43),[System.Drawing.PointF]::new(61,65),[System.Drawing.PointF]::new(68,54),[System.Drawing.PointF]::new(77,54))
    $graphics.DrawLines($pen,$points)
    $stream=[System.IO.MemoryStream]::new()
    $bitmap.Save($stream,[System.Drawing.Imaging.ImageFormat]::Png)
    $imageBuffers+=,@{Size=$iconSize;Bytes=$stream.ToArray()}
    if($iconSize -eq 256){[System.IO.File]::WriteAllBytes((Join-Path $iconRoot 'fiteasy.png'),$stream.ToArray())}
    $stream.Dispose();$pen.Dispose();$gold.Dispose();$green.Dispose();$leaf.Dispose();$heart.Dispose();$edge.Dispose();$white.Dispose();$outer.Dispose();$graphics.Dispose();$bitmap.Dispose()
}
$icoStream=[System.IO.File]::Create((Join-Path $iconRoot 'fiteasy.ico'))
$writer=[System.IO.BinaryWriter]::new($icoStream)
try {
    $writer.Write([uint16]0);$writer.Write([uint16]1);$writer.Write([uint16]$imageBuffers.Count)
    $offset=6+16*$imageBuffers.Count
    foreach($entry in $imageBuffers){$dimension=if($entry.Size -eq 256){0}else{$entry.Size};$writer.Write([byte]$dimension);$writer.Write([byte]$dimension);$writer.Write([byte]0);$writer.Write([byte]0);$writer.Write([uint16]1);$writer.Write([uint16]32);$writer.Write([uint32]$entry.Bytes.Length);$writer.Write([uint32]$offset);$offset+=$entry.Bytes.Length}
    foreach($entry in $imageBuffers){$writer.Write([byte[]]$entry.Bytes)}
}finally{$writer.Dispose();$icoStream.Dispose()}
