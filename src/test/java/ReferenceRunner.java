import java.util.LinkedList;

public class ReferenceRunner {

//	public static void main(String[] args) throws Exception
//	{
//		
//		Reference r = new Reference();
//		r.Diff_Timeout = 0f;
//////		String first = "Now is the time for all good men to come to the aid of their country";
//////		String second = "Then was the time for all good men to have come to the aid of their country";
//
//////		List<Diff> lst = d.diff_main(first,second,false, Long.MAX_VALUE);
//////		System.out.println( lst );
////		
//		Patch patch = new Patch();
//		String middle = "Knowing that the time had come for her to leave this world, where she "
//				+ "had been within such a short space of time a wife, a mother, and a "
//				+ "widow, she went to her room, where slept her son George, guarded by "
//				+ "waiting women.  He was three years old; his long eyelashes threw a "
//				+ "pretty shade on his cheeks, and his mouth was like a flower.  Seeing how "
//				+ "small he was and how young, she began to cry.";
//		
//		String left = "This eBook is for the use of anyone anywhere in the United States and most"
//				+ " other parts of the world at no cost and with almost no restrictions "
//				+ "whatsoever.  You may copy it, give it away or re-use it under the terms of"
//				+ " the Project Gutenberg License included with this eBook or online at "
//				+ "www.gutenberg.org.  If you are not located in the United States, you'll have "
//				+ "to check the laws of the country where you are located before using this ebook.";
//		
//		String right = "'Legal Entity' shall mean the union of the acting entity and all "
//				+ "other entities that control, are controlled by, or are under common "
//				+ "control with that entity. For the purposes of this definition, "
//				+ "'control' means (i) the power, direct or indirect, to cause the "
//				+ "direction or management of such entity, whether by contract or "
//				+ "otherwise, or (ii) ownership of fifty percent (50%) or more of the "
//				+ "outstanding shares, or (iii) beneficial ownership of such entity.";
//		
//		LinkedList<Diff> ll = new LinkedList<Diff>();
//		ll.add( new Diff( Operation.INSERT, right ) );
//		ll.add( new Diff( Operation.EQUAL, middle ) );
//		ll.add( new Diff( Operation.DELETE, left ) );		
//		
//		LinkedList<Patch> lp = r.patch_make( ll );
//		System.out.println( lp );
//		
////		patch.length1 = left.length();
////		patch.length2 = right.length();
////		patch.start1 = 0;
////		patch.start2 = 0;
////		patch.diffs.add( new Diff( Operation.DELETE, left ) );
////		patch.diffs.add( new Diff( Operation.EQUAL, middle ) );
////		patch.diffs.add( new Diff( Operation.INSERT, right ) );
////		
////		LinkedList<Patch> patches = new LinkedList<>();
////		patches.add( patch );
////		
////		Object[] results = r.patch_apply( patches, middle+left );
////		System.out.println( results[0] );
////		
//		LoggingSetup.setupLogging("DEBUG");
////		URL url1 = Thread.currentThread().getContextClassLoader().getResource("dataOld");
////		
////		SpanBuffer buff = SpanBuffer.Factory.wrap(url1.openStream());
////		String buffer1 = buff.getText();
////		
////		URL url2 = Thread.currentThread().getContextClassLoader().getResource("dataNew");
////		
////		buff = SpanBuffer.Factory.wrap(url2.openStream());
////		String buffer2 = buff.getText();
////		
//
////		LinkedList<Diff> ll = r.diff_main( buffer1,  buffer2, false);
////		
////		for (Diff d : ll)
////		{
////			System.out.println( String.format( "%s %s", d.operation, d.text.length()));
////		}
////		
////		LinkedList<Patch> pl = r.patch_make( ll );
////		System.out.println( pl );
////		
////		System.out.println( r.patch_toText(pl));
////		
//
////	
////		LinkedList<Diff> di = new LinkedList<Diff>();
////		di.add(new Diff(Operation.EQUAL, "Now is the time for all good "));
////		di.add(new Diff(Operation.INSERT, "wo"));
////		di.add(new Diff(Operation.EQUAL, "men to come to the aid of their country."));
////		
////		LinkedList<Patch> pl = r.patch_make(di);
////	
////	
////				String s = pl.toString();
////				System.out.println( s );
//		
//		
//		
////		ByteArrayOutputStream baos = new ByteArrayOutputStream();
////		p.write(baos);
////		assertArrayEquals( TEST_STRING.getBytes(), baos.toByteArray() );
////		
////		@@ -516,65 +516,8 @@
////		 .bin
////		-%0Aboot system flash c2800nm-advipservicesk9-mz.124-32a.bin
////		 %0D%0Abo
////			
////		String txt = "Knowing that the time had come for her to leave this world, where she";
////		Patch pf = new Patch();
////		pf.start1=516;
////		pf.start2=516;
////		pf.length1=65;
////		pf.length2=8;
////		pf.diffs.add( new Diff( Operation.EQUAL, ".bin") );
////		pf.diffs.add( new Diff( Operation.DELETE, "xboot system flash c2800nm-advipservicesk9-mz.124-32a.bin" ));
////		pf.diffs.add( new Diff( Operation.EQUAL, "xxAbo" ));
////		
////		LinkedList<Patch> frags = new LinkedList<Patch>();
////		frags.add( pf );
////		
////		System.out.println( frags );
////		
////		System.out.println( "================");
////		
////		Reference r = new Reference();
////		
////		r.patch_splitMax(frags);
////		System.out.println( frags );
//	}
//	

	public static void main(String[] args) throws Exception {
		System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");

		Reference r = new Reference();

//		 dmp.Patch_DeleteThreshold = 0.5f;
		//
//				    // Compensate for failed patch.
//				    dmp.Match_Threshold = 0.0f;
//				    dmp.Match_Distance = 0;
//				    patches = dmp.patch_make("abcdefghijklmnopqrstuvwxyz--------------------1234567890", "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
//				    results = dmp.patch_apply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");
//				    boolArray = (boolean[]) results[1];
//				    resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
//				    assertEquals("patch_apply: Compensate for failed patch.", "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567YYYYYYYYYY890\tfalse\ttrue", resultStr);

		LinkedList<Reference.Patch> patches = r
				.patch_make("x1234567890123456789012345678901234567890123456789012345678901234567890y", "xabcy");
		for (Reference.Patch p : patches) {
			System.out.println(p.toString());
		}
		r.Match_Threshold = 0.5f;
		r.Match_Distance = 1000;
		Object[] results = r.patch_apply(patches,
				"x12345678901234567890---------------++++++++++---------------12345678901234567890y");

//		assertEquals(1, result.getUsed().cardinality());
//		assertFalse(result.getUsed().get(0));
//		assertTrue(result.getUsed().get(1));
//		assertEquals("xabc12345678901234567890---------------++++++++++---------------12345678901234567890y",
//				result.getResult().getText());

//	    LinkedList<Reference.Patch> patches = r.patch_make("abcdefghijklmnopqrstuvwxyz--------------------1234567890", "abcXXXXXXXXXXdefghijklmnopqrstuvwxyz--------------------1234567YYYYYYYYYY890");
//	    for (Reference.Patch p : patches)
//	    {
//	    	System.out.println( p.toString() );
//	    }
//	    r.Match_Threshold = 0.0f;
//	    r.Match_Distance = 0;
//	    Object[] results = r.patch_apply(patches, "ABCDEFGHIJKLMNOPQRSTUVWXYZ--------------------1234567890");

//	    results = dmp.patch_apply(patches, "The quick red rabbit jumps over the tired tiger.");
		boolean[] boolArray = (boolean[]) results[1];
		String resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
		System.out.println("Results: " + resultStr);
		boolean matches = "xabc12345678901234567890---------------++++++++++---------------12345678901234567890y"
				.equals(results[0].toString());
		System.out.println("Match: " + matches);
//	    assertEquals("patch_apply: Partial match.", "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr);

		System.out.println("++++++++++++++ NEW ++++++++++++++");
		for (Reference.Patch p : patches) {
			System.out.println(p.toString());
		}
//	    results = dmp.patch_apply(patches, "The quick red rabbit jumps over the tired tiger.");
//	    boolArray = (boolean[]) results[1];
//	    resultStr = results[0] + "\t" + boolArray[0] + "\t" + boolArray[1];
//	    assertEquals("patch_apply: Partial match.", "That quick red rabbit jumped over a tired tiger.\ttrue\ttrue", resultStr);

		// System.out.println( r.patch_toText(patches));
		// assertEquals("patch_splitMax: #1.", "@@ -1,32 +1,46 @@\n+X\n ab\n+X\n
		// cd\n+X\n ef\n+X\n gh\n+X\n ij\n+X\n kl\n+X\n mn\n+X\n op\n+X\n qr\n+X\n
		// st\n+X\n uv\n+X\n wx\n+X\n yz\n+X\n 012345\n@@ -25,13 +39,18 @@\n zX01\n+X\n
		// 23\n+X\n 45\n+X\n 67\n+X\n 89\n+X\n 0\n", dmp.patch_toText(patches));

//		}
//		
//		Object[] out = r.patch_apply(lp, newB.getText());
//		if ( ! out[0].toString().equals( oldB.getText() ))
//		{
//			System.out.println( "patch does not work");
//		}
//		
//		ApplyResult result = patch.apply(newB);

	}

}
