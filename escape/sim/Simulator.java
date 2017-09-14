package escape.sim;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Simulator {
    private static final String root = "escape";
    private static final String statics_root = "statics";
    private static final int turnLimit = 100000;

    private static long playerTimeout = 5000;
    private static boolean gui = false;
    private static double fps = 5;
    private static int n = 0;
    private static List<String> playerNames = new ArrayList<String>();
    private static PlayerWrapper[] players;

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
//		args = new String[] {"-p", "random", "random", "random", "-v", "-g"};
        parseArgs(args);
        n = playerNames.size();
        int[] lastMoves = new int[n];
        players = new PlayerWrapper[n];
        for (int i = 0; i < n; ++ i) {
        	Log.record("Loading player " + (i + 1) + ": " + playerNames.get(i));
        	Player player = loadPlayer(playerNames.get(i));
        	if (player == null) {
        		Log.record("Cannot load player " + (i + 1) + ": " + playerNames.get(i));
        		System.exit(1);
        	}
        	players[i] = new PlayerWrapper(player, i, playerNames.get(i), playerTimeout);
        }

        System.out.println("Starting game with " + n + " players");

        boolean success = false;
        int[] move = new int[n];
        // List<Integer>[] handles = new List<Integer>[n];
        List<List<Integer>> handles = new ArrayList<List<Integer>>();
        HTTPServer server = null;
        if (gui) {
        	server = new HTTPServer();
        	Log.record("Hosting HTTP Server on " + server.addr());
        	if (!Desktop.isDesktopSupported())
        		Log.record("Desktop operations not supported");
        	else if (!Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
        		Log.record("Desktop browse operation not supported");
			else {
				try {
					Desktop.getDesktop().browse(new URI("http://localhost:" + server.port()));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
        }

        for (int i = 0; i < n; ++ i) {
        	try {
        		move[i] = players[i].init(n);
        	} catch (Exception e) {
        		e.printStackTrace();
        		System.exit(1);
        	}
        	if (move[i] < 0 || move[i] >= n) {
        		System.out.println("Player " + (i + 1) + " makes an illegal move.");
        		System.exit(1);
        	}
        	// handles.get(i) = new ArrayList<Integer>();
          handles.add(new ArrayList<Integer>());
        }

        for (int turn = 1; turn < turnLimit; ++ turn) {
        	System.out.println("Round " + turn);
        	for (int i = 0; i < n; ++ i)
        		handles.get(i).clear();
        	success = true;
        	for (int i = 0; i < n; ++ i) {
        		System.out.println("Player " + (i + 1) + " attempts to hold handle " + (move[i] + 1));
        		handles.get(move[i]).add(i);
        		if (handles.get(move[i]).size() > 1)
        			success = false;
        	}

    			for (int i = 0; i < n; ++ i) {
    				if (handles.get(i).size() == 0) continue;
            if (handles.get(i).size() == 1) {
              players[handles.get(i).get(0)].release();
            }
    				System.out.print("Attempting handle " + (i + 1) + ":");
    				for (int k : handles.get(i))
    					System.out.print(" " + (k + 1));
    				System.out.println("");
    				//System.out.println(" grabbed handle " + i);
    			}

        	if (gui) gui(server, state(n, playerNames, handles, success ? -1 : fps, turn));
        	if (success) {
        		System.out.println("Every player has a unique handle.");
        		System.out.println("Scores: " + turn);
        		break;
        	}
        	for (int i = 0; i < n; ++ i) {
        		try {
    					move[i] = players[i].attempt(handles.get(move[i]));
    				} catch (Exception e) {
    					e.printStackTrace();
    					System.exit(1);
    				}
          	if (move[i] < 0 || move[i] >= n) {
          		System.out.println("Player " + (i + 1) + "(" + players[i].getName() + ") makes an illegal move.");
          		System.exit(1);
          	}
        	}
        }

        if (!success) {
        	System.out.println("You are doomed!");
        }
        for (int i = 0; i < n; ++ i) {
        	Log.record("Total running time for player " + (i + 1) + " is " + players[i].getTotalElapsedTime() + "ms");
        }
        System.exit(0);
    }

    private static String state(int n, List<String> playerNames, List<List<Integer>> handles, double fps, int turn) {
    	// TODO
    	double refresh = 1000.0 / fps;
    	String ret = refresh + "," + turn + "," + n;
      for (int i = 0; i < n; ++ i)
        ret += "," + playerNames.get(i);
    	for (int i = 0; i < n; ++ i) {
    		ret += "," + handles.get(i).size();
    		for (Integer j : handles.get(i))
    			ret += "," + j;
    	}
    	return ret;
    }

    private static void gui(HTTPServer server, String content) {
    	if (server == null) return;
    	String path = null;
    	for (;;) {
    		for (;;) {
    			try {
    				path = server.request();
    				break;
    			} catch (IOException e) {
    				Log.record("HTTP request error " + e.getMessage());
    			}
    		}
    		if (path.equals("data.txt")) {
    			try {
    				server.reply(content);
    			} catch (IOException e) {
    				Log.record("HTTP dynamic reply error " + e.getMessage());
    			}
				return;
    		}
    		if (path.equals("")) path = "webpage.html";
    		else if (!Character.isLetter(path.charAt(0))) {
    			Log.record("Potentially malicious HTTP request \"" + path + "\"");
    			break;
    		}

    		File file = new File(statics_root + File.separator + path);
    		if (file == null) {
    			Log.record("Unknown HTTP request \"" + path + "\"");
    		} else {
    			try {
    				server.reply(file);
    			} catch (IOException e) {
    				Log.record("HTTP static reply error " + e.getMessage());
    			}
    		}
    	}
    }

    private static void parseArgs(String[] args) {
        int i = 0;
        for (; i < args.length; ++i) {
            switch (args[i].charAt(0)) {
                case '-':
                    if (args[i].equals("-p") || args[i].equals("--players")) {
                        while (i + 1 < args.length && args[i + 1].charAt(0) != '-') {
                            ++i;
                            playerNames.add(args[i]);
                        }
                    } else if (args[i].equals("-g") || args[i].equals("--gui")) {
                        gui = true;
                    } else if (args[i].equals("-t") || args[i].equals("--timelimit")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit");
                        }
                        playerTimeout = Integer.parseInt(args[i]);
                    } else if (args[i].equals("-l") || args[i].equals("--logfile")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing logfile name");
                        }
                        Log.setLogFile(args[i]);
                    } else if (args[i].equals("--fps")) {
                        if (++i == args.length) {
                            throw new IllegalArgumentException("Missing time limit");
                        }
                        fps = Double.parseDouble(args[i]);
                    } else if (args[i].equals("-v") || args[i].equals("--verbose")) {
                    	Log.activate();
                    } else {
                        throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument \"" + args[i] + "\"");
            }
        }

        if (playerNames.size() < 1) {
            throw new IllegalArgumentException("Not enough players, you need at least 1 player to start a game");
        }
        Log.record("Number of players: " + playerNames.size());
        Log.record("Time limit for each player: " + playerTimeout + "ms");
        Log.record("GUI " + (gui ? "enabled" : "disabled"));
        if (gui)
            Log.record("FPS: " + fps);
    }
    private static Set<File> directory(String path, String extension) {
        Set<File> files = new HashSet<File>();
        Set<File> prev_dirs = new HashSet<File>();
        prev_dirs.add(new File(path));
        do {
            Set<File> next_dirs = new HashSet<File>();
            for (File dir : prev_dirs)
                for (File file : dir.listFiles())
                    if (!file.canRead()) ;
                    else if (file.isDirectory())
                        next_dirs.add(file);
                    else if (file.getPath().endsWith(extension))
                        files.add(file);
            prev_dirs = next_dirs;
        } while (!prev_dirs.isEmpty());
        return files;
    }

    public static Player loadPlayer(String name) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String sep = File.separator;
        Set<File> player_files = directory(root + sep + name, ".java");
        File class_file = new File(root + sep + name + sep + "Player.class");
        long class_modified = class_file.exists() ? class_file.lastModified() : -1;
        if (class_modified < 0 || class_modified < last_modified(player_files) ||
                class_modified < last_modified(directory(root + sep + "sim", ".java"))) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null)
                throw new IOException("Cannot find Java compiler");
            StandardJavaFileManager manager = compiler.
                    getStandardFileManager(null, null, null);
//            long files = player_files.size();
            Log.record("Compiling for player " + name);
            if (!compiler.getTask(null, manager, null, null, null,
                    manager.getJavaFileObjectsFromFiles(player_files)).call())
                throw new IOException("Compilation failed");
            class_file = new File(root + sep + name + sep + "Player.class");
            if (!class_file.exists())
                throw new FileNotFoundException("Missing class file");
        }
        ClassLoader loader = Simulator.class.getClassLoader();
        if (loader == null)
            throw new IOException("Cannot find Java class loader");
        @SuppressWarnings("rawtypes")
        Class raw_class = loader.loadClass(root + "." + name + ".Player");
        return (Player)raw_class.newInstance();
    }

    private static long last_modified(Iterable<File> files) {
        long last_date = 0;
        for (File file : files) {
            long date = file.lastModified();
            if (last_date < date)
                last_date = date;
        }
        return last_date;
    }
}
