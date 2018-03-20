# build

```
mvn package shade:shade
```

# デプロイ
```
sls deploy -v
sls deploy -v --stage prod
```

# ローカル実行
```
sls invoke local -f default
```

# 実行
```
sls invoke -f default
```

# ログ
```
sls logs -f default -t
```

# 削除
```
sls remove -v
```
