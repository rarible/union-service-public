### Release 1.28 ###

```
PUT _cluster/settings
{
    "persistent": {
        "action.auto_create_index": "false"
    }
}
```