#!/bin/bash
#
# Usage: mkinbox.sh event-file outdir
#
# The generated outdir should be placed on your SOLIDBASE
#

SOLIDBASE="https://bellow2.ugent.be/test/scholix"
WEBID="https://bellow2.ugent.be/test/profile/card#me"
EMAIL="test@test.edu"

function show_help {
    echo "usage: $0 [-b <solidbase>] [-e <email>] [-w webid] event-file outdir"
}

while getopts "b:e:w:" opt; do
    case "$opt" in
    b)  SOLIDBASE=$OPTARG
        ;;
    e)  EMAIL=$OPTARG
        ;;
    w)  WEBID=$OPTARG
        ;;
    esac
done

shift $((OPTIND-1))
[ "${1:-}" = "--" ] && shift

EVENTFILE=$1
OUTDIR=$2

if [[ "${EVENTFILE}" == "" ]] || [[ "${OUTDIR}" == "" ]]; then
    show_help
    exit 1
fi

survey() {
    read -p "$1 (y/N) " answer

    if [ "${answer}" == "y" ]; then 
        :
    else
        exit 0
    fi
}

doit() {
    echo "mkdir $1"
    mkdir -p $1
}

export -f doit

if [ ! -d ${OUTDIR} ]; then
    mkdir -p ${OUTDIR}
else
    survey "Clean ${OUTDIR}/*?"
    rm -rf ${OUTDIR}/*
    rm ${OUTDIR}/.acl
fi

cat ${EVENTFILE} | jq -r '.target.inbox' | sort -u |\
    sed -e "s|${SOLIDBASE}||" | xargs -n 1 -I {} bash -c 'doit "$@"' _ "${OUTDIR}{}"

echo "Generating ${OUTDIR}/.acl"

cat <<EOF > ${OUTDIR}/.acl
# Root ACL resource for the agent account
@prefix acl: <http://www.w3.org/ns/auth/acl#>.
@prefix foaf: <http://xmlns.com/foaf/0.1/>.

# The homepage is readable by the public
<#public>
    a acl:Authorization;
    acl:agentClass foaf:Agent;
    acl:accessTo <./>;
    acl:default <./>;
    acl:mode acl:Read , acl:Append.

# The owner has full access to every resource in their pod.
# Other agents have no access rights,
# unless specifically authorized in other .acl resources.
<#owner>
    a acl:Authorization;
    acl:agent <${WEBID}>;
    # Optional owner email, to be used for account recovery:
    acl:agent <mailto:${EMAIL}>;
    # Set the access to the root storage folder itself
    acl:accessTo <./>;
    # All resources will inherit this authorization, by default
    acl:default <./>;
    # The owner has all of the access modes allowed
    acl:mode
        acl:Read, acl:Write, acl:Control.
EOF

echo "Ok. Move now \`${OUTDIR}\` contents to \`${SOLIDBASE}\`."