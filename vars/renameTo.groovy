import org.liferay.sdlc.FileOperations;

def call(from, to) {
    new FileOperations().renameTo(from, to);
}
