# Tailscale Operator install (one-time, cluster-wide)

Run once before applying `k8s/base/`.

```bash
# 1. Create OAuth client in https://login.tailscale.com/admin/settings/oauth
#    Scopes: "Devices: Core" + "Devices: Auth Keys: write"
#    Tag the client: tag:k8s-operator
#
# 2. Create the namespace + secret with the OAuth credentials
kubectl create namespace tailscale
kubectl create secret generic operator-oauth -n tailscale \
  --from-literal=client_id="$TS_CLIENT_ID" \
  --from-literal=client_secret="$TS_CLIENT_SECRET"

# 3. Install the operator via Helm
helm repo add tailscale https://pkgs.tailscale.com/helmcharts
helm repo update
helm upgrade --install tailscale-operator tailscale/tailscale-operator \
  --namespace tailscale \
  --set-string oauth.clientId="$TS_CLIENT_ID" \
  --set-string oauth.clientSecret="$TS_CLIENT_SECRET" \
  --set-string apiServerProxyConfig.mode=true
```

## ACL — add to your tailnet policy

```hujson
{
  "tagOwners": {
    "tag:k8s-operator": ["autogroup:admin"],
    "tag:k8s":          ["tag:k8s-operator"]
  },
  "acls": [
    { "action": "accept", "src": ["autogroup:member"], "dst": ["tag:k8s:*"] }
  ]
}
```

After install, `api-tailscale.yaml` exposes the API on the tailnet at `https://memes-api.<tailnet>.ts.net`.
