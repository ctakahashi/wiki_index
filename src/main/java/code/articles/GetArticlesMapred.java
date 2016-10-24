package code.articles;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import code.util.WikipediaPageInputFormat;
import edu.umd.cloud9.collection.wikipedia.WikipediaPage;

/**
 * This class is used for Section A of assignment 1. You are supposed to
 * implement a main method that has first argument to be the dump wikipedia
 * input filename , and second argument being an output filename that only
 * contains articles of people as mentioned in the people auxiliary file.
 */
public class GetArticlesMapred {

	//@formatter:off
	/**
	 * Input:
	 * 		Page offset 	WikipediaPage
	 * Output
	 * 		Page offset 	WikipediaPage
	 * @author Tuan
	 *
	 */
	//@formatter:on
	public static class GetArticlesMapper extends Mapper<LongWritable, WikipediaPage, Text, Text> {
		
		//a hashset to store the names that we're interested in
		private HashSet<String> names;
		
		@Override
		protected void setup(Mapper<LongWritable, WikipediaPage, Text, Text>.Context context)
				throws IOException, InterruptedException {
			
			ClassLoader cl = GetArticlesMapred.class.getClassLoader();
			
			//load people.txt
			String PEOPLE_FILE = "people.txt";
			String peopleUrl = cl.getResource(PEOPLE_FILE).getFile();
			
			//create a jar file that contains people.txt so that docker can find it
			String peopleJarUrl = peopleUrl.substring(5, peopleUrl.length() - PEOPLE_FILE.length() - 2);
			JarFile peopleJar = new JarFile(new File(peopleJarUrl));
			
			//set up a scanner to look at people.txt within the jar file
			Scanner sc = new Scanner(peopleJar.getInputStream(peopleJar.getEntry(PEOPLE_FILE)));
			
			names = new HashSet<String>();
			String name;
			
			//add all names from people.txt that are two words long to the hash set
			while(sc.hasNextLine()) {
				name = sc.nextLine();
				names.add(name);
			}
			sc.close();
			
			//this should go last in the method?
			super.setup(context);
		}

		@Override
		public void map(LongWritable offset, WikipediaPage inputPage, Context context)
				throws IOException, InterruptedException {
			
			String title = inputPage.getTitle();
			
			//add if this section of the title is one of the names from people.txt
			if(names.contains(title)) {
				//map the title text and the page text
				Text pageText = new Text();
				pageText.set(inputPage.getRawXML());
							
				context.write(new Text(), pageText);
			}
			
		}
	}

	public static void main(String[] args) throws Exception {
		
		Configuration conf = new Configuration();
		
		//set up the arguments given by the user
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		
		Job job = Job.getInstance(conf);
		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		
		job.setJarByClass(GetArticlesMapred.class);
		job.setMapperClass(GetArticlesMapper.class);
		
		job.setInputFormatClass(WikipediaPageInputFormat.class);
		job.setOutputFormatClass(TextOutputFormat.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		
		try {
			System.exit(job.waitForCompletion(true) ? 0 : 1);
		} catch (ClassNotFoundException e) {
			
		} catch (InterruptedException e) {
			
		}
	}
}
