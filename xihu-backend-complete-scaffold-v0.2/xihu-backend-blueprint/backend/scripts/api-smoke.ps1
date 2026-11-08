$ErrorActionPreference='Stop'
$root='http://localhost:8080/api/v1'
$body=@{username='admin'; password='DemoPass123!'} | ConvertTo-Json
$login=Invoke-RestMethod -Uri "$root/auth/login" -Method Post -ContentType 'application/json' -Body $body
$headers=@{ Authorization="Bearer $($login.data.accessToken)" }
$me=Invoke-RestMethod -Uri "$root/admin/me" -Headers $headers
$created=Invoke-RestMethod -Uri "$root/admin/events" -Method Post -ContentType 'application/json' -Headers $headers -Body (@{title='AI 安全论坛 - Demo'} | ConvertTo-Json)
$id=$created.data.id
$detail=Invoke-RestMethod -Uri "$root/admin/events/$id" -Headers $headers
if ($detail.data.id -ne $id) { throw "Resource read failed" }
$published=Invoke-RestMethod -Uri "$root/admin/events/$id/publish" -Method Post -Headers $headers
$list=Invoke-RestMethod -Uri "$root/public/events"
if (-not ($list.data | Where-Object {$_.id -eq $id})) { throw 'Published event is not in public list' }
Write-Host "PASS: Login, /admin/me, create, publish and list; ID=$id"
