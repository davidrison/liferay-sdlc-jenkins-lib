import org.liferay.sdlc.FileOperations;

def call(path) {
    new FileOperations().removeRecursively(path);
}
