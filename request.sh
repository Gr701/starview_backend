#!/bin/bash

server="https://localhost:8001"

if [ $# -gt 0 ]; then
    case $1 in
        1|post)
            echo "Posting empty..."
            curl --include --insecure -X POST "$server/datarecord"
            ;;

        2|post_json)
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

        put_json)
            echo "Putting json..."
            curl --include --insecure -v -u user2:password2 -X PUT "$server/datarecord?id=1" \
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
        "latitude": "2",
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

        4|get)
            echo "Getting datarecord..."
            curl --include --insecure -v -u user2:password2 -X GET "$server/datarecord"
            ;;

        5|search)
            echo "Searching..."
            #the second argument should be 
            # ?identification=Jupiter&nickname=Name&before=2027-04-16T15:43:53.722Z
            # time is compared just as strings
            curl --include --insecure -u user2:password2 -X GET "$server/search$2"
            ;;
        6|profile)
            echo "Getting profile..."
            curl --include --insecure -u user2:password2 -X GET "$server/profile"
            ;;
    esac
fi
exit 0 


###
POST https://localhost:8001/datarecord
Content-Type: application/json
Authorization: Basic dummy:passwd

{
    "recordIdentifier" : "Jupiter",
    "recordDescription": "Notes about Jupiter during observation",
    "recordPayload": "Jupiter great red spot noticed, no changes related to previousobservation",
    "recordRightAscension" : "05h 11m 42s",
    "recordDeclination": "+22° 15' 54\""
}

###
GET https://localhost:8001/datarecord
Authorization: Basic dummy:passwd

###
DELETE https://localhost:8001/datarecord


###
GET https://localhost:8001/datarecord
Authorization: Basic user2:password2

###
GET https://localhost:8001

###
POST https://localhost:8001/datarecord
Content-Type: application/json
Authorization: Basic user2:password2

{
    "recordRightAscension":{},
    "recordPayload":{},
    "recordDeclination":0.5654151282359118,
    "recordDescription":{},
    "recordIdentifier":0.8177455329831433
}

###
POST https://localhost:8001/datarecord
Content-Type: application/json
Authorization: Basic user2:password2

{
"recordRightAscension":"4h 14m 33s",
"recordPayload":{},
"recordDeclination":"+15° 45' 2\"",
"recordDescription":{},
"recordIdentifier":{}
}

###
GET https://localhost:8001/search
Authorization: Basic user2:password2
