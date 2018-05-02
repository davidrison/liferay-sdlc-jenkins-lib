def call(where, commands)
{
    sh "ssh -o BatchMode=yes -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $where \"$commands\""
}
