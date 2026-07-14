Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('d:\DAD\project\nutritional-llm-service\BITP 3123 - Sen 2 2025 2026 - Project - Artefact Declaration Document - GroupNo.docx')
$entry = $zip.GetEntry('word/document.xml')
$stream = $entry.Open()
$reader = New-Object System.IO.StreamReader($stream)
$xmlString = $reader.ReadToEnd()
$reader.Close()
$stream.Close()
$zip.Dispose()
$xml = [xml]$xmlString
$xml.DocumentElement.InnerText
