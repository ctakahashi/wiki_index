package code.inverted;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.fs.Path;

import code.util.StringIntegerList;
import code.util.StringIntegerList.StringInteger;

/*
 * This class is used for Section C.2 of assignment 1. You are supposed to run
 * the code taking the lemma index filename as input, and output being the
 * inverted index.
 */

public class InvertedIndexMapred {

	/* 
	 * This class creates an index that maps an article to a <lemma, frequency> pair  
	 * 
	 * Input:
	 * article1 <lemma1, frequency> <lemma2, frequency>
	 * 
	 * Output:
	 * lemma1 <article1, frequency>
	 * lemma1 <article2, frequency>
	 */	
	public static class InvertedIndexMapper extends Mapper<Text, Text, Text, StringInteger> {
		public static Text lemma = new Text();
		public static String articleIdKey;

		public void map(Text articleId, Text indices, Context context) throws IOException, InterruptedException {
			articleId = new Text(articleId.toString().trim());
			indices = new Text(indices.toString().trim());
			StringIntegerList lemma_freqs = new StringIntegerList();
			lemma_freqs.readFromString(indices.toString());
			// For each StringInteger, extract the lemma and frequency.
			for (StringInteger lemma_freq : lemma_freqs.getIndices()) {
				lemma.set(lemma_freq.getString().trim());
				context.write(lemma, new StringInteger(articleId, lemma_freq.getValue()));
			}
		}
	}

	/* 
	 * This class creates the inverted index for one lemma mapping lemma to the article and frequency. 
	 * 
	 * Input:
	 * lemma1 <article1, frequency>, <article2, frequency>
	 * 
	 * Output:
	 * lemma1 {<article1, frequency>, <article2, frequency>}
	 */	
	public static class InvertedIndexReducer extends Reducer<Text, StringInteger, Text, StringIntegerList> {

		public void reduce(Text lemma, Iterable<StringInteger> articlesAndFreqs, Context context)
				throws IOException, InterruptedException {
			lemma = new Text(lemma.toString().trim());
			List<StringInteger> temp = new ArrayList<StringInteger>();
			for (StringInteger articleAndFreq : articlesAndFreqs) {
				temp.add(new StringInteger(articleAndFreq.getString(), articleAndFreq.getValue()));
			}
			context.write(lemma, new StringIntegerList(temp));
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();

		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();


		Job job = Job.getInstance(conf, "inverted index");
		job.setJarByClass(InvertedIndexMapred.class);
		job.setMapperClass(InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndexReducer.class);
		
		// Read key/value pair as input.	
		job.setInputFormatClass(KeyValueTextInputFormat.class);

		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(StringInteger.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(StringIntegerList.class);

		FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
		FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
}

