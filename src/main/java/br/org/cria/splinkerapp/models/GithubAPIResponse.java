package br.org.cria.splinkerapp.models;

import java.util.ArrayList;
import java.util.List;

public class GithubAPIResponse {

    String name;
    List<GithubAsset> assets = new ArrayList();
    public GithubAPIResponse() {}
    
    public GithubAPIResponse(String name, List<GithubAsset> assets) {
        this.name = name;
        this.assets = assets;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public List<GithubAsset> getAssets() {
        return assets;
    }

    public void addAssets(GithubAsset asset) {
        this.assets.add(asset);
    }
    
    public void removeAsset(String name)
    {
        this.assets.removeIf((e) -> e.name == name);
    }
    
}
