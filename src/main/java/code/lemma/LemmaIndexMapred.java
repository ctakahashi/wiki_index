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
		public Map<String, Integer> index = new HashMap<String, Integer>(); //map for counting lemma frequency
		private Tokenizer t = new Tokenizer();
		
		@Override
		public void map(LongWritable offset, WikipediaPage page, Context context) throws IOException,
				InterruptedException {
			// TODO: implement Lemma Index mapper here
			
			//tokenizes the WikipediaPage
			String text = page.getContent();
			String title = page.getTitle();
			List<String> list = t.tokenize(text);
				
			for(String str : list) {
//				lemma.set(str);
//				if(!index.isEmpty() && index.containsKey(lemma.toString())) {
//					index.put(lemma.toString(), index.get(lemma.toString()) + 1);
//				} else {
//					index.put(lemma.toString(), 1);
//				}
				if(!index.isEmpty() && index.containsKey(str)) {
					index.put(str, index.get(str) + 1);
				} else {
					index.put(str, 1);
				}
			} 
			context.write(new Text(title), new StringIntegerList(index));
//			context.write(new Text(title), new Text(text));
		
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

