package com.tron.explorer.controller;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.helper.StringUtil;
import org.tron.common.crypto.ECKey;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jfinal.aop.Before;
import com.jfinal.core.Controller;
import com.tron.explorer.Constrain;
import com.tron.explorer.encrypt.Base58;
import com.tron.explorer.interceptor.AuthInterceptor;
import com.tron.explorer.model.Account;
import com.tron.explorer.model.BaseQuery;
import com.tron.explorer.model.Transaction;
import com.tron.explorer.model.Transactions;
import com.tron.explorer.model.TronException;
import com.tron.explorer.service.AccountService;
import com.tron.explorer.service.WalletService;
import com.tron.explorer.util.CheckUtil;
import com.tron.explorer.util.DecodeUtil;
import com.tron.explorer.util.PageUtil;
import com.tron.explorer.util.StringUtils;
import com.tron.explorer.util.Utils;

public class WalletController extends Controller {
	
	@Before(AuthInterceptor.class)
	public void index() throws TronException {
		String address = (String) this.getSession().getAttribute("address");
		Account account = AccountService.getAccountByAddress(address);
		Long currPageIndex = PageUtil.getPage(getPara("page"));	
		BaseQuery baseQuery = new BaseQuery();
		baseQuery.setLimit(Constrain.pageSize);
		baseQuery.setOffset((currPageIndex-1)*Constrain.pageSize);
		baseQuery.setSort("-timestamp");
		Transactions transactions = AccountService.queryTransactionList(address,baseQuery);
		if(transactions == null || transactions.getCount() <= 0){
			setAttr("transcount", 0);
		}else{			
			List<Transaction> innerList = transactions.getOrders();		
			setAttr("transcount", transactions.getCount());
			setAttr("tranactions", innerList);
			setAttr("currpage",currPageIndex);
			setAttr("totalpage",transactions.getCount()/Constrain.pageSize + 1);
		}
		setAttr("account", account);
		setAttr("voteList", account.getVotes());
		setAttr("frozenList", account.getFrozens());
		Map<String, Object> assetMap = new HashMap<String, Object>();
		assetMap.put("TRX", account.getBalance());
		JSONArray assetArray = new JSONArray();
		JSONObject obj = new JSONObject();
		 obj.put("name", "TRX 【" + account.getBalance() + "】");
	     obj.put("value", "TRX");
	     assetArray.add(obj);
		if(account.getAssets() != null){
			Iterator<Entry<String, Long>> it = account.getAssets().entrySet().iterator(); 
			   while(it.hasNext()){  
			     Entry<String, Long> entry=it.next();  
			     assetMap.put(entry.getKey(), String.valueOf(entry.getValue()));
			     obj = new JSONObject();
			     obj.put("name", entry.getKey() + " 【" + entry.getValue() + "】");
			     obj.put("value", entry.getKey());
			     assetArray.add(obj);
			 }  
		}
		setAttr("assertMap",assetMap);		
		setAttr("assertJson",assetArray.toString());
		render("wallet/index.html");
	}
	
	public void login() throws TronException {
		
		render("wallet/login.html");
	}
	
	public void logout() throws TronException {
		if(this.getSession().getAttribute("address") != null){
			this.getSession().removeAttribute("address");
		}
		renderJson();
	}
	
	public void doLogin() throws TronException {
		String password = getPara("password");
		String address = getPara("address");
		JSONObject obj = new JSONObject();
		if(StringUtils.isBlank(password) || password.length() < 40){
			obj.put("status", 0);
		}
		Account account = WalletService.getAccountByPassword(password);
		if(account == null){
			obj.put("status", 0);
		}else{
			obj.put("status", 1);
			if(StringUtil.isBlank(address)){
				address = account.getAddress();
			}
			this.getSession().setAttribute("address", address);
			this.getSession().setAttribute("password", password);
		}
		obj.put("password", password);
		renderJson(obj);
	}
	
	public void generate() throws TronException {
		ECKey ecKey = new ECKey(Utils.getRandom());
		JSONObject obj = new JSONObject();
		obj.put("address", Base58.encodeChecked(ecKey.getAddress()));
		obj.put("password", DecodeUtil.bytesToHex(ecKey.getPrivKeyBytes()));
		renderJson(obj);
	}
	
	@Before(AuthInterceptor.class)
	public void send() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		String fromAddress = (String) this.getSession().getAttribute("address");
		String password = (String) this.getSession().getAttribute("password");
		String toAddress = getPara("toAddress");
		String amount = getPara("amount");
		String asset = getPara("asset");
		long amountToLong = (long) (Double.valueOf(amount) * 1000000);
		if(!asset.equals("TRX")){
			amountToLong = Long.valueOf(amount);			
		}
		if(!CheckUtil.isValidAddress(fromAddress) && !CheckUtil.isValidAddress(toAddress)){
			renderJson(obj);
			return;
		}
		boolean result = WalletService.send(asset,fromAddress, toAddress, amountToLong,password);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);
	}
	
	@Before(AuthInterceptor.class)
	public void getFreeTRX() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		String fromAddress = (String) this.getSession().getAttribute("address");
		boolean result = WalletService.getFreeTRX(fromAddress);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);
	}
	
	@Before(AuthInterceptor.class)
	public void vote() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		String addressList = getPara("addressList");
		String amountList = getPara("amountList");
		if(StringUtil.isBlank(addressList) || StringUtil.isBlank(amountList)){
			renderJson(obj);
			return;
		}
		HashMap<String, String> witness = new HashMap<String, String>();
		for(int i=0,length=addressList.split(",").length; i<length; i++){
			witness.put(addressList.split(",")[i], amountList.split(",")[i]);
		}
		String fromAddress = (String) this.getSession().getAttribute("address");
		String password = (String) this.getSession().getAttribute("password");
		boolean result = WalletService.vote(fromAddress,password,witness);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);
	}
	
	@Before(AuthInterceptor.class)
	public void assetBuy() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		String assetname = getPara("assetname");
		String toaddress = getPara("toaddress");
		String amountpara = getPara("amount");
		long amount = (long) ((Float.valueOf(amountpara) * Constrain.ONE_TRX));
		if(StringUtil.isBlank(toaddress) || !CheckUtil.isValidAddress(toaddress)){
			renderJson(obj);
			return;
		}
		String fromAddress = (String) this.getSession().getAttribute("address");
		String password = (String) this.getSession().getAttribute("password");
		boolean result = WalletService.assetBuy(assetname,fromAddress,toaddress,amount,password);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);

	}
	
	@Before(AuthInterceptor.class)
	public void freeze() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);
		
		String frozenBalance = getPara("frozenBalance");
		String fromAddress = (String) this.getSession().getAttribute("address");
		String password = (String) this.getSession().getAttribute("password");
		long frozenDuration = 3L;
		long frozen_balance = (long) ((Double.valueOf(frozenBalance) * Constrain.ONE_TRX));
		boolean result = WalletService.freeze(fromAddress,frozen_balance,frozenDuration,password);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);		
	}
	
	@Before(AuthInterceptor.class)
	public void unfreeze() throws TronException {
		JSONObject obj = new JSONObject();
		obj.put("code", 0);		
		String fromAddress = (String) this.getSession().getAttribute("address");
		String password = (String) this.getSession().getAttribute("password");
		boolean result = WalletService.unfreeze(fromAddress,password);
		if(result){
			obj.put("code", 1);
		}
		renderJson(obj);		
	}
}
 