package pt.inescid.safecloudfs.utils;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class SafeCloudFSOptions extends OptionsBase {



	@Option(
			name = "mount",
			abbrev = 'm',
			help = "Directory to be mount.",
			defaultValue = "")
	public String mountDirectory;

	@Option(
			name = "config",
			abbrev = 'c',
			help = "Config file path.",
			defaultValue = "")
	public String configFile;

	@Option(
			name = "accessKeys",
			abbrev = 'a',
			help = "Cloud access keys file.",
			defaultValue = "")
	public String cloudAccessKeysFile;

	@Option(
			name = "depspace",
			abbrev = 'h',
			help = "Depspace hosts file.",
			defaultValue = "")
	public String depspaceHostsFile;

	@Option(
			name = "zookeeper",
			abbrev = 'z',
			help = "Zookeeper server address.",
			defaultValue = "")
	public String zookeeperAddress;

	@Option(
			name = "debug",
			abbrev = 'd',
			help = "Execute with debug log messages (ALL, SIMPLE, WARNING, SEVERE, INFO, FINE, FINER, FINEST).",
			defaultValue = "")
	public String debug;


	@Option(
			name = "recovery",
			abbrev = 'r',
			help = "Opens a GUI that allows intrusion recovery.",
			defaultValue = "false")
	public boolean recovery;

	@Option(
			name = "cache",
			abbrev = 'x',
			help = "Path to a folder to store cached files.",
			defaultValue = "")
	public String cache;


}
