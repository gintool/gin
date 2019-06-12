#!/bin/sh
# From gist at https://gist.github.com/chadmaughan/5889802

# this hook is in SCM so that it can be shared
# to install it, create a symbolic link in the projects .git/hooks folder
#
#       i.e. - from the .git/hooks directory, run
#               $ ln -s ../../git-hooks/pre-commit.sh pre-commit
#
# to skip the tests, run with the --no-verify argument
#       i.e. - $ 'git commit --no-verify'

echo "Precommit Hook running gradle tests"

# stash any unstaged changes
#git stash -q --keep-index

# run the tests with the gradle wrapper
gradle test

# store the last exit code in a variable
RESULT=$?

# unstash the unstashed changes
#git stash pop -q

# return the './gradlew test' exit code
exit $RESULT
