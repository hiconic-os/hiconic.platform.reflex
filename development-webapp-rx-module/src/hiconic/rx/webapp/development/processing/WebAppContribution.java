package hiconic.rx.webapp.development.processing;

public record WebAppContribution(String dependency, String part, String serverPath, String welcomeFile, String source) {

	public String declaration() {
		String result = dependency + "/" + part + "=" + serverPath;
		return welcomeFile == null ? result : result + ";welcome=" + welcomeFile;
	}

	public boolean sameMapping(WebAppContribution other) {
		return dependency.equals(other.dependency) && part.equals(other.part) && serverPath.equals(other.serverPath)
				&& java.util.Objects.equals(welcomeFile, other.welcomeFile);
	}
}
