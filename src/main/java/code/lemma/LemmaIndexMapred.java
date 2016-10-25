package code.lemma;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;

import code.util.StringIntegerList;
import code.util.WikipediaPageInputFormat;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;

//import util.StringIntegerList;
import edu.umd.cloud9.collection.wikipedia.WikipediaPage;

/**
 * Input: One WikipediaPage at a time
 *
 * Output: title <lemma, freq>, ...
 */
public class LemmaIndexMapred {
	public static class LemmaIndexMapper extends Mapper<LongWritable, WikipediaPage, Text, StringIntegerList> {
		private Tokenizer t = new Tokenizer();
		
		@Override
		public void map(LongWritable offset, WikipediaPage page, Context context) throws IOException,
				InterruptedException {
			// Create a Guava Multiset for efficient counting
			Multiset<String> index = HashMultiset.create();
			// Tokenize the page content (which removes wiki markup) and add to multiset
			index.addAll(t.tokenize(page.getContent()));
			Map<String, Integer> map = new HashMap<>();
			// Iterate through multiset and add words and counts to HashMap
			for(String word : index.elementSet()) {
				map.put(word, index.count(word));
			}
			context.write(new Text(page.getTitle()), new StringIntegerList(map));
		}
	}
	
		public static void main(String[] args) throws Exception {
		  Configuration conf = new Configuration();

		  String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		  
		  Job job = Job.getInstance(conf);
		  job.setJarByClass(LemmaIndexMapred.class);
		  job.setMapperClass(LemmaIndexMapper.class);
		  job.setInputFormatClass(WikipediaPageInputFormat.class);
		  job.setMapOutputKeyClass(Text.class);
		  job.setMapOutputValueClass(StringIntegerList.class);
		  FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		  FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
		  System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

