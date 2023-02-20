sh ./cleanall.sh
echo "compiling cblas"
cd cblas
sh ./compilesource.sh
cd ..
echo "compiling dlopen"
cd dlopen
sh ./compilesource.sh
cd ..
echo "compiling go"
cd go
sh ./compilesource.sh
cd ..
echo "compiling helloworld"
cd helloworld
sh ./compilesource.sh
cd ..
echo "compiling lapack"
cd lapack
sh ./compilesource.sh
cd ..
echo "compiling libclang"
cd libclang
sh ./compilesource.sh
cd ..
echo "compiling libcurl"
cd libcurl
sh ./compilesource.sh
cd ..
echo "compiling libffmpeg"
cd libffmpeg
sh ./compilesource.sh
cd ..
echo "compiling libjimage"
cd libjimage
sh ./compilesource.sh
cd ..
echo "compiling libgit2"
cd libgit2
sh ./compilesource.sh
cd ..
echo "compiling libproc"
cd libproc
sh ./compilesource.sh
cd ..
echo "compiling lp_solve"
cd lp_solve
sh ./compilesource.sh
cd ..
echo "compiling opengl"
cd opengl
sh ./compilesource.sh
cd ..
echo "compiling pcre2"
cd pcre2
sh ./compilesource.sh
cd ..
echo "compiling python3"
cd python3
sh ./compilesource.sh
cd ..
echo "compiling readline"
cd readline
sh ./compilesource.sh
cd ..
echo "compiling sqlite"
cd sqlite
sh ./compilesource.sh
cd ..
echo "compiling tcl"
cd tcl
sh ./compilesource.sh
cd ..
echo "compiling tensorflow"
cd tensorflow
sh ./compilesource.sh
cd ..
echo "compiling time"
cd time
sh ./compilesource.sh
cd ..
echo "compiling libzstd"
cd libzstd
sh ./compilesource.sh
cd ..
