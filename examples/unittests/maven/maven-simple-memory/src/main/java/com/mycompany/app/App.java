package com.mycompany.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Makes a load of objects of different types
 */
public class App
{
    public static void main(String[] args)
    {
        make10KIntArrays();
        make100KIntArrays();
        make1000KIntArrays();
    }

    public static int make10KIntArrays() {

    	List<int[]> l = new ArrayList<>();
    	
        for (int i = 0; i < 10000; i++) {
        	l.add(new int[100]);
        	l.get(l.size() - 1);
        }
        
        int total = 0;
        for (int[] i : l) {
        	for (int ii : i) {
        		total += ii;
        	}
        }
        
        return total;
        
    }

    public static int make100KIntArrays() {

    	List<int[]> l = new ArrayList<>();
    	
        for (int i = 0; i < 100000; i++) {
        	l.add(new int[100]);
        	l.get(l.size() - 1);
        }
        
        int total = 0;
        for (int[] i : l) {
        	for (int ii : i) {
        		total += ii;
        	}
        }
        
        return total;

    }

    public static int make1000KIntArrays() {

    	List<int[]> l = new ArrayList<>();
    	
        for (int i = 0; i < 1000000; i++) {
        	l.add(new int[100]);
        	l.get(l.size() - 1);
        }
        
        int total = 0;
        for (int[] i : l) {
        	for (int ii : i) {
        		total += ii;
        	}
        }
        
        return total;

    }


}
