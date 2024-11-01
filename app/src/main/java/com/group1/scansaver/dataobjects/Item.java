package com.group1.scansaver.dataobjects;

public class Item {

    private String ITEM_UPC;
    private String ITEM_NAME;
    private double ITEM_PRICE;
    private String[] ITEM_LOCATION_ARRAY;


    public Item(){
        this.ITEM_UPC = null;
        this.ITEM_NAME = null;
        this.ITEM_PRICE = 0.00;
        this.ITEM_LOCATION_ARRAY = null;
    }

}
