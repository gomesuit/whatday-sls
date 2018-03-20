# build

```
mvn package shade:shade
```

# デプロイ
```
sls deploy -v
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
