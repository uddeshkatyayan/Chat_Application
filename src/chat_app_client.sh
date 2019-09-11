#!/bin/bash
# Bash Menu Script Example

PS3='Please enter your choice: '
options=("Option 1" "Option 2" "Option 3" "Quit")
select opt in "${options[@]}"
do
    case $opt in
        "Option 1")
            echo "you chose choice 1"
            cd ass2_withoutEncrypt
            javac client.java
            java TCPClient $1 $2
            cd ..
            break
            ;;
        "Option 2")
            echo "you chose choice 2"
            cd ass2_withoutSignature
            javac client.java
            java TCPClient $1 $2
            cd ..
            break
            ;;
        "Option 3")
            echo "you chose choice $REPLY which is $opt"
            cd ass2_withSignature
            javac client.java
            java TCPClient $1 $2 
            cd ..
            break
            ;;
        "Quit")
            break
            ;;
        *) echo "invalid option $REPLY";;
    esac
done

