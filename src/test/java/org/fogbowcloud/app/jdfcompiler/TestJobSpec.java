package org.fogbowcloud.app.jdfcompiler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.fogbowcloud.app.jdfcompiler.job.JobSpecification;
import org.fogbowcloud.app.jdfcompiler.main.DescriptionFileCompile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestJobSpec {

	public static final String EXSIMPLE_JOB = "examples" + File.separator + "addJob" + File.separator + "simplejob.jdf";

	public static final String EXSIMPLEST_JOB = "examples" + File.separator + "addJob" + File.separator + "simplestjob.jdf";

	
	public static final String EXSIMPLE_JOB2 = "examples" + File.separator + "addJob" + File.separator
			+ "simplejob2.jdf";

	public static final String EXSIMPLE_JOB3 = "examples" + File.separator + "addJob" + File.separator
			+ "simplejob3.jdf";
	
	public static final String EXSIMPLE_JOB4 = "examples" + File.separator + "addJob" + File.separator
			+ "NewSimpleJob.jdf";
	
	public static final String EXSIMPLE_JOB5 = "examples" + File.separator + "addJob" + File.separator
			+ "NewSimpleJob2.jdf";
	
	public static final String EXSIMPLE_JOB6 = "examples" + File.separator + "addJob" + File.separator
			+ "NewSimpleJob3.jdf";
	
	public static final String EXSIMPLE_JOB7 = "examples" + File.separator + "addJob" + File.separator
			+ "NewSimpleJob4.jdf";
	

	public static final String EXSIMPLE_JOB8 = "examples" + File.separator + "addJob" + File.separator
			+ "FJob.jdf";
	
	
	public static final String RESOURCE_DIR = "test" + File.separator + "resources";

	
	public static final String SIMPLE_JOB1 = RESOURCE_DIR + File.separator + "SimpleJob1.jdf";

	public static final String SIMPLE_JOB2 = RESOURCE_DIR + File.separator + "SimpleJob2.jdf";

	public static final String SIMPLE_JOB3 = RESOURCE_DIR + File.separator + "SimpleJob3.jdf";

	public static final String SIMPLE_JOB4 = RESOURCE_DIR + File.separator + "SimpleJob4.jdf";
			
	public static final String SIMPLE_TAGS_JOB1 = RESOURCE_DIR + File.separator + "SimpleJob1WithTags.jdf";
	
	public static final String SIMPLE_TAGS_JOB2 = RESOURCE_DIR + File.separator + "SimpleJob2WithTags.jdf";
	
	public static final String SIMPLE_TAGS_JOB3 = RESOURCE_DIR + File.separator + "SimpleJob3WithTags.jdf";

	public static final String SIMPLE_MP_JOB = RESOURCE_DIR + File.separator + "SimpleMPJob.jdf";

	public static final String LARGE_JOB = RESOURCE_DIR + File.separator + "jobv4.jdf";


	@Before
	public void setUp( ) throws Exception {

		BasicConfigurator.resetConfiguration();
		BasicConfigurator.configure();
		Configuration.getInstance( FakeConfiguration.FAKE );
	}


	@After
	public void tearDown( ) throws Exception {

		System.gc();
		BasicConfigurator.resetConfiguration();
	}


	@Test
	public void testExSimpleJob( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB );
		assertEquals( "SimpleJob", spec.getLabel() );		
		assertTrue( 1 == spec.getTaskSpecs().size() );

	}

	@Test
	public void testSimplestJob( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLEST_JOB );
		assertEquals( "SimpleJob", spec.getLabel() );		
		assertTrue( 1 == spec.getTaskSpecs().size() );

	}
	
	
	@Test
	public void testExSimpleJob2( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB2 );
		assertEquals( "SimpleJob2", spec.getLabel() );
		// yes the label for SimpleJob2 has the same name as in SimpleJob
		assertTrue( 2 == spec.getTaskSpecs().size() );
	}


	@Test
	public void testExSimpleJob3( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB3 );
		assertEquals( "SimpleJob3", spec.getLabel() );
		assertTrue( 1 == spec.getTaskSpecs().size() );

	}


	@Test
	public void testSimpleJob1( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_JOB1 );
		assertEquals( "SimpleJob", spec.getLabel() );
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}


	@Test
	public void testSimpleJob2( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_JOB2 );
		assertEquals( "SimpleJob2", spec.getLabel() );
		assertTrue( 3 == spec.getTaskSpecs().size() );
	}


	@Test
	public void testSimpleJob3( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_JOB3 );
		assertEquals( "SimpleJob", spec.getLabel() );
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}


	@Test
	public void testSimpleJob4( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_JOB4 );
		assertEquals( "SimpleJob4", spec.getLabel() );
		assertTrue( 1 == spec.getTaskSpecs().size() );
	}


	@Test
	public void testLargeJob( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( LARGE_JOB );
		assertEquals( "bsdata", spec.getLabel() );
		assertTrue( 200 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testSimpleTagsJob1( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_TAGS_JOB1 );
		assertEquals( "SimpleJob", spec.getLabel() );		
		assertTrue( 1 == spec.getAnnotations().size() );		
		assertTrue( spec.getAnnotations().containsKey( "tag1" ) );
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testSimpleTagsJob2( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_TAGS_JOB2 );
		assertEquals( "SimpleJob2", spec.getLabel() );
		
		assertTrue( 5 == spec.getAnnotations().size() );		
		assertTrue( spec.getAnnotations().containsKey( "quein" ) );
		assertTrue( spec.getAnnotations().containsKey( "quein2" ) );
		assertTrue( spec.getAnnotations().containsKey( "quein3" ) );
		assertTrue( spec.getAnnotations().containsKey( "queinN" ) );	
		assertTrue( spec.getAnnotations().containsKey( "quein6" ) );
		assertTrue( 3 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testSimpleTagsJob3( ) throws Exception {
		JobSpecification spec = DescriptionFileCompile.compileJDF( SIMPLE_TAGS_JOB3 );
		System.out.println(spec.getAnnotations().size()); //TODO Remove
		assertTrue( 3 == spec.getAnnotations().size() );
		assertTrue( spec.getAnnotations().containsKey( "tag1" ) );
		assertTrue( spec.getAnnotations().containsKey( "tag2" ) );
		assertTrue( spec.getAnnotations().containsKey( "tag3" ) );		
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}

	@Test
	public void testNewJobClausesMultiplePutOnInit( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB4 );
		assertEquals( "SimpleJob4", spec.getLabel() );		
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testNewJobRunningThingsOnInit( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB5 );
		assertEquals( "SimpleJob4", spec.getLabel() );		
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testNewJobRunningMoreThanOneLineOnRemote( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB6 );
		assertEquals( "SimpleJob4", spec.getLabel() );		
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}
	
	
	@Test
	public void testNewJobRunningPutOnRemote( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB7 );
		assertEquals( "SimpleJob4", spec.getLabel() );
		spec.getTaskSpecs().get(0).getInitBlock();
		assertTrue( 4 == spec.getTaskSpecs().size() );
	}
	
	@Test
	public void testNewFJob( ) throws Exception {

		JobSpecification spec = DescriptionFileCompile.compileJDF( EXSIMPLE_JOB8 );
		assertEquals( "SimpleJob4", spec.getLabel() );
		spec.getTaskSpecs().get(0).getInitBlock();
		assertTrue( 3 == spec.getTaskSpecs().size() );
	}
	
	
	
	
}