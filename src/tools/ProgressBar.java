package tools;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Original method by Periklis Ntanasis (MasterEx@github)
 * Ascii progress meter. On completion this will reset itself,
 * so it can be reused
 * <br /><br />
 * 100% ################################################## |
 */
public class ProgressBar {
	private StringBuilder progress = new StringBuilder();
	long total = 0;
	AtomicLong done = new AtomicLong(0);
	AtomicLong percent = new AtomicLong(0);

	/**
	 * initialize progress bar properties.
	 */
	public ProgressBar() {
		init();
	}

	public ProgressBar(int total) {
		init();
		this.total = total;
	}

	//    public void setTotal(int total){
	//    	this.total = total;
	//    }
	/**
	 * called whenever the progress bar needs to be updated.
	 * that is whenever progress was made.
	 *
	 * @param done an int representing the work done so far
	 * @param total an int representing the total work
	 */
	public void update(long done, long total) {
		if (total > 0) {
			if (done >= 0) 	{
				this.done = new AtomicLong(done);
			}
			//    	if (done < 0) System.out.println("ProgresBar: Received done of value: "+done);
			char[] workchars = {'|', '/', '-', '\\'};
			String format = "\r%3d%% %s %c";

			long percent = (this.done.getAndIncrement() * 100) / total;

			if (percent > this.percent.get()){
				long extrachars = (percent / 2) - this.progress.length();

				while (extrachars-- > 0) {
					progress.append('#');
				}

				System.out.printf(format, percent, progress,
						workchars[(int) (this.done.get() % workchars.length)]);
			}
			this.percent.set(percent);
			if (this.done.get() == total) {
				System.out.flush();
				System.out.println();
				init();
			}
		}

	}

	public void updatePlusPlus(){
		update( -1, total);
	}

	private void init() {
	}
}
