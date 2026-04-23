#!/bin/bash

server="https://localhost:8001"

if [ $# -gt 0 ]; then
    case $1 in
        post)
            echo "Posting empty..."
            curl --include --insecure -u user1:password1 -X POST "$server/datarecord"
            ;;

        post_json1)
            echo "Posting json..."
            curl --include --insecure -v -u user1:password1 -X POST "$server/datarecord" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "recordIdentifier": "First UUUSER post",
    "recordDescription": "my day was foo",
    "recordPayload": "Picture here",
    "recordRightAscension": "05h 189m 42s",
    "recordDeclination": "+22° 15' 54\""
}
EOF
            ;;

        post_json2)
            echo "Posting json..."
            curl --include --insecure -v -u user2:password2 -X POST "$server/datarecord" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "recordIdentifier": "Jupiter",
    "recordDescription": "Notes about Jupiter during observation",
    "recordPayload": "Jupiter great red spot noticed, no changes related to previousobservation",
    "recordRightAscension": "05h 11m 42s",
    "recordDeclination": "+22° 15' 54\""
}
EOF
            ;;

        put_json1)
            echo "Putting json..."
            curl --include --insecure -v -u user1:password1 -X PUT "$server/datarecord?recordId=1" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "editType": "editRecord",
    "updateReason": "testiiiiiing",
    "recordIdentifier": "JupiterBoba",
    "recordDescription": "Notes about Jupiter during observation",
    "recordPayload": "Jupiter great red spot noticed, no changes related to previousobservation",
    "recordRightAscension": "05h 11m 42s",
    "recordDeclination": "+22° 15' 54\"",
    "observatory": {
        "observatoryName": "boba_telescopa",
        "latitude": 90,
        "longitude": "2.1241421"
    },
}
EOF
            ;;

        put_json2)
            echo "Putting json..."
            curl --include --insecure -v -u user2:password2 -X PUT "$server/datarecord?recordId=1" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "editType": "editRecord",
    "updateReason": "testiiiiiing",
    "recordIdentifier": "JupiterBoba",
    "recordDescription": "Notes about Jupiter during observation",
    "recordPayload": "Jupiter great red spot noticed, no changes related to previousobservation",
    "recordRightAscension": "05h 11m 42s",
    "recordDeclination": "+22° 15' 54\"",
    "observatory": {
        "observatoryName": "boba_telescopa",
        "latitude": 90,
        "longitude": "2.1241421"
    },
}
EOF
            ;;

        add_view)
            echo "Adding view json..."
            curl --include --insecure -v -u user1:password1 -X PUT "$server/datarecord?id=1" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "editType": "addView",
}
EOF
            ;;

        rate)
            echo "Adding view json..."
            curl --include --insecure -v -u user1:password1 -X PUT "$server/datarecord?id=1" \
                -H "Content-Type: application/json" \
                -d @- <<EOF
{
    "editType": "updateRating",
    "rating": "1",
}
EOF
            ;;
        
        reg_1)
            echo "Registering user1..."
            curl --include --insecure -X POST "$server/registration" \
                -H "Content-Type: application/json" \
                -d @- <<EOF       
{
    "username": "user1",
    "password": "password1",
    "email": "",
    "userNickname": "sisiki"
}
EOF
            ;;

        reg_2)
            echo "Registering user2..."
            curl --include --insecure -X POST "$server/registration" \
                -H "Content-Type: application/json" \
                -d @- <<EOF       
{
    "username": "user2",
    "password": "password2",
    "email": "",
    "userNickname": "Balalako"
}
EOF
            ;;

        get)
            echo "Getting datarecord..."
            curl --include --insecure -v -u user2:password2 -X GET "$server/datarecord"
            ;;

        search)
            echo "Searching..."
            #the second argument should be 
            # ?identification=Jupiter&nickname=Name&before=2027-04-16T15:43:53.722Z
            # time is compared just as strings
            curl --include --insecure -u user2:password2 -X GET "$server/search$2"
            ;;

        profile)
            echo "Getting profile..."
            curl --include --insecure -u user1:password1 -X GET "$server/profile"
            ;;

        comment1)
            echo "Commenting..."
            curl --include --insecure -u user1:password1 -X POST "$server/comment" \
                -H "Content-Type: application/json" \
                -d @- <<EOF       
{
    "recordId": 1.3,
    "text": "my very first comment here :)"
}
EOF
            ;;

        comment2)
            echo "Commenting..."
            curl --include --insecure -u user2:password2 -X POST "$server/comment" \
                -H "Content-Type: application/json" \
                -d @- <<EOF       
{
    "recordId": 1,
    "text": "my very second comment here :)"
}
EOF
            ;;

        get_comments)
            echo "Getting comments..."
            curl --include --insecure -v -u user2:password2 -X GET "$server/comment?recordId=1"
            ;;

        delete)
            echo "Deleting record..."
            curl --include --insecure -v -u user2:password2 -X DELETE "$server/datarecord?recordId=3"
            ;;
    esac
fi
exit 0 



###
DELETE https://localhost:8001/datarecord
