import org.liferay.sdlc.FileOperations;

def call(path) {
    new FileOperations().mkdir(path);
}
