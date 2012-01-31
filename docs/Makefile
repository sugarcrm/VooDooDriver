# Makefile to create the VooDooDriver documentation

MAKEINFO := $(shell which makeinfo)
TEXI2PDF := $(shell which texi2pdf)

SOURCE = voodoo.texi
HTML   = voodoo.html
PDF    = voodoo.pdf
INFO   = voodoo.info

.PHONY: all check kinda-clean clean

all : check ${PDF} ${HTML} ${INFO}

check :
	if [ -z ${MAKEINFO} ] ; then echo "(!)Missing makeinfo"; /bin/false; fi
	if [ -z ${TEXI2PDF} ] ; then echo "(!)Missing texi2pdf"; /bin/false; fi

${PDF} : ${SOURCE}
	${TEXI2PDF} $<
${HTML} : ${SOURCE}
	${MAKEINFO} --html --no-split $<
${INFO} : ${SOURCE}
	${MAKEINFO} $<

kinda-clean :
	rm -f *.aux *.cp *.fn *.ky *.log *.pg *.toc *.tp *.vr
clean : kinda-clean
	rm -f ${PDF} ${HTML}
