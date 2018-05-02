def call(files, destination) {
    sh "scp -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $files $destination"
}
