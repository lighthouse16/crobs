Set-Location $PSScriptRoot
if (!(Test-Path out)) { New-Item -ItemType Directory -Path out | Out-Null }

Write-Host "[1/3] Compiling..." -ForegroundColor Cyan
$sources = Get-ChildItem -Recurse 'src\*.java','test\*.java' | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp "lib/*;src;test" -d out $sources

if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed." -ForegroundColor Red
    exit 1
}

Write-Host "[2/3] Running tests..." -ForegroundColor Cyan
java -cp "out;lib/*" TestRunner

Write-Host "[3/3] Starting CROBS GUI..." -ForegroundColor Green
Start-Process javaw -ArgumentList '-cp "out;lib/*" hk.edu.polyu.comp.comp2021.crobs.gui.CrobsGuiApplication'
