package com.mycompany.app;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{

	@org.junit.Test
    public void test10KIntArray() throws Exception {
        assertEquals(0, App.make10KIntArrays());
    }
	
	@org.junit.Test
    public void test100KIntArray() throws Exception {
        assertEquals(0, App.make100KIntArrays());
    }
	
	@org.junit.Test
    public void test1000KIntArray() throws Exception {
        assertEquals(0, App.make1000KIntArrays());
    }

 
}
