package com.tron.explorer.service;

import java.util.Optional;

import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.walletserver.WalletClient;

import com.tron.explorer.model.Asset;
import com.tron.explorer.model.Assets;
import com.tron.explorer.model.TronException;
import com.tron.explorer.util.PropUtil;

public class AssetService extends BaseService {
	
	public static Assets queryAsserts() throws TronException{
		return queryAsserts(true);
	}
	
	public static Assets queryAsserts(boolean isMainnet) throws TronException{
		Optional<AssetIssueList> assetList;
		if(!isMainnet){
			assetList = WalletClient.getAssetIssueTestList();
		}else{
			assetList = WalletClient.getAssetIssueList();
		}
		if(!assetList.isPresent()){
			return new Assets();
		}
		return new Assets(assetList.get());		
	}
	
	public static Asset getAssetByName(String name) throws TronException{
		return new Asset(client.get(
				PropUtil.getValue("baseNewURL") + "/api/token/" + name));		
	}
	
	public static Asset getTestAssetByName(String name) throws TronException{
		return new Asset(client.get(
				PropUtil.getValue("baseNewTestURL") + "/api/token/" + name));		
	}
	
	public static String getAssetTrans(String name) throws TronException{
		return client.get(
				PropUtil.getValue("baseNewURL") + "/api/transfer?sort=-timestamp&count=true&limit=25&start=0&token=" + name);		
	}
	
	public static String getAssetTops(String name) throws TronException{
		return client.get(
				PropUtil.getValue("baseNewURL") + "/api/token/" + name + "/address?sort=-balance&limit=50");		
	}

	public static Assets queryTestAsserts(String address) throws TronException {
		return new Assets(client.get(
				PropUtil.getValue("baseNewTestURL") + "/api/token?sort=-name&limit=40&start=0"),address);		
	}
}
