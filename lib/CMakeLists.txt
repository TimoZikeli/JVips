include(ExternalProject)

# Read external project versions
file(STRINGS ${CMAKE_CURRENT_SOURCE_DIR}/VERSIONS VERSIONS_LIST)
foreach(ITEM ${VERSIONS_LIST})
    string(REPLACE "=" ";" ARGS ${ITEM})
    list(GET ARGS 0 LIB)
    list(GET ARGS 1 VERSION)
    set(${LIB} ${VERSION})
endforeach()

set(VIPS_VERSION ${VIPS_VERSION} PARENT_SCOPE)
string(REPLACE "." "_" EXIF_VERSION_TAG ${EXIF_VERSION})

# Set the host target
if (NOT DEFINED BUILD_TARGET)
  set(CONFIGURE_HOST "--host=x86_64-pc-linux")
elseif(${BUILD_TARGET} STREQUAL "w64")
  set(CONFIGURE_HOST "--host=x86_64-w64-mingw32")
endif()

if (NOT DEFINED CMAKE_BUILD_TYPE)
set(CONFIGURE_CFLAGS "${CMAKE_C_FLAGS}")
set(CONFIGURE_CXXFLAGS "${CMAKE_CXX_FLAGS}")
elseif (${CMAKE_BUILD_TYPE} STREQUAL "Release")
  set(CONFIGURE_CFLAGS ${CMAKE_C_FLAGS_RELEASE})
  set(CONFIGURE_CXXFLAGS ${CMAKE_CXX_FLAGS_RELEASE})
elseif (${CMAKE_BUILD_TYPE} STREQUAL "Debug")
  set(CONFIGURE_CFLAGS ${CMAKE_C_FLAGS_DEBUG})
  set(CONFIGURE_CXXFLAGS ${CMAKE_CXX_FLAGS_DEBUG})
endif()

# Workaround to set more than one compiler flags
string(REPLACE " " "\ " CONFIGURE_CFLAGS "${CONFIGURE_CFLAGS}")
string(REPLACE " " "\ " CONFIGURE_CXXFLAGS "${CONFIGURE_CXXFLAGS}")

list(APPEND CONFIGURE_VARS
  PKG_CONFIG_PATH=${EXT_INSTALL_DIR}/lib/pkgconfig:$PKG_CONFIG_PATH
  CFLAGS=${CONFIGURE_CFLAGS}
  CXXFLAGS=${CONFIGURE_CXXFLAGS}
  LDFLAGS=-L${EXT_INSTALL_DIR}/lib,-Wl,-rpath=${EXT_INSTALL_DIR}/lib
  CPPFLAGS=-I${EXT_INSTALL_DIR}/include
  --prefix=${EXT_INSTALL_DIR}
  ${CONFIGURE_HOST}
  )

find_library(LIBIMAGEQUANT imagequant PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if (NOT LIBIMAGEQUANT)
    # https://github.com/ImageOptim/libimagequant/issues/36
    if (CMAKE_COMPILER_IS_GNUCC AND CMAKE_C_COMPILER_VERSION VERSION_GREATER 7.0 AND ${BUILD_TARGET} STREQUAL "linux")
        set(LIQ_OPENMP "--with-openmp")
    else()
        set(LIQ_OPENMP "--without-openmp")
    endif()
    ExternalProject_Add(libimagequant
      URL "https://github.com/ImageOptim/libimagequant/archive/${LIQ_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libimagequant"
      CONFIGURE_COMMAND ./configure CC=${CMAKE_C_COMPILER} CFLAGS=${CONFIGURE_CFLAGS}
      --prefix=${EXT_INSTALL_DIR}
      --with-sse
      ${LIQ_OPENMP}
      BUILD_IN_SOURCE 1
      )

    if (${BUILD_TARGET} STREQUAL "w64")
      ExternalProject_Add_Step(libimagequant rename-dll
      COMMAND mv libimagequant.so libimagequant.dll
      WORKING_DIRECTORY ${EXT_INSTALL_DIR}/lib/
      DEPENDEES install
      )
      ExternalProject_Add_StepTargets(libimagequant rename-dll) 
    endif()
else()
    add_custom_target(libimagequant "")
endif()

find_library(LIBEXIF exif PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT LIBEXIF)
    ExternalProject_Add(libexif
      URL "https://github.com/libexif/libexif/releases/download/libexif-${EXIF_VERSION_TAG}-release/libexif-${EXIF_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libexif"
      CONFIGURE_COMMAND ${CMAKE_CURRENT_BINARY_DIR}/libexif/src/libexif/configure
      ${CONFIGURE_VARS}
      --enable-shared
      --disable-static
      --disable-docs
      )
else()
    add_custom_target(libexif "")
endif()

find_library(LIBJPEG jpeg turbojpeg PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT LIBJPEG)
    ExternalProject_Add(libjpeg
      URL "https://github.com/libjpeg-turbo/libjpeg-turbo/archive/${JPG_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libjpeg-turbo"
      PATCH_COMMAND autoreconf -fiv
      CONFIGURE_COMMAND ./configure
      ${CONFIGURE_VARS}
      --enable-shared
      --disable-static
      --with-jpeg8
      BUILD_IN_SOURCE 1
      )
else()
    add_custom_target(libjpeg "")
endif()

find_library(LIBPNG png PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT LIBPNG)
    ExternalProject_Add(libpng
      URL "https://downloads.sourceforge.net/libpng/libpng-${PNG_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libpng"
      CONFIGURE_COMMAND ${CMAKE_CURRENT_BINARY_DIR}/libpng/src/libpng/configure
      ${CONFIGURE_VARS}
      --enable-shared
      --disable-static
      --disable-dependency-tracking
      --disable-silent-rules
      )
else()
    add_custom_target(libpng "")
endif()

find_library(GIFLIB gif PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if (NOT GIFLIB)
    # giflib hasn't a standard build system, don't append CONFIGURE_VARS
    ExternalProject_Add(giflib
      URL "https://downloads.sourceforge.net/project/giflib/giflib-${GIF_VERSION}.tar.bz2"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/giflib"
      CONFIGURE_COMMAND ${CMAKE_CURRENT_BINARY_DIR}/giflib/src/giflib/configure
      --prefix=${EXT_INSTALL_DIR}
      ${CONFIGURE_HOST}
      --enable-shared
      --disable-static
      --disable-dependency-tracking
      )
else()
    add_custom_target(giflib "")
endif()

find_library(LIBWEBP webp webpmux webpdemux PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT LIBWEBP)
    ExternalProject_Add(libwebp
      URL "http://downloads.webmproject.org/releases/webp/libwebp-${WEBP_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libwebp"
      CONFIGURE_COMMAND ${CMAKE_CURRENT_BINARY_DIR}/libwebp/src/libwebp/configure
      ${CONFIGURE_VARS}
      --with-gifincludedir=${EXT_INSTALL_DIR}/include
      --with-giflibdir=${EXT_INSTALL_DIR}/lib
      --with-jpegincludedir=${EXT_INSTALL_DIR}/include
      --with-jpeglibdir=${EXT_INSTALL_DIR}/lib
      --with-pngincludedir=${EXT_INSTALL_DIR}/include
      --with-pnglibdir=${EXT_INSTALL_DIR}/lib
      --enable-shared
      --disable-static
      --disable-dependency-tracking
      --enable-libwebpmux
      --without-orc
      --disable-assert
      --enable-avx2
      --enable-sse4.1
      --enable-sse2
      --enable-threading
      DEPENDS libjpeg libpng giflib
      )
endif()

find_library(LCMS2 lcms2 EXACT PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT LCMS2)
    ExternalProject_Add(lcms2
      URL "https://github.com/mm2/Little-CMS/archive/lcms${LCMS2_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/lcms2"
      CONFIGURE_COMMAND ${CMAKE_CURRENT_BINARY_DIR}/lcms2/src/lcms2/configure
      ${CONFIGURE_VARS}
      --enable-shared
      --disable-static
      --disable-dependency-tracking
      )
else()
    add_custom_target(lcms2 "")
endif()

find_library(VIPS vips PATHS "${EXT_INSTALL_DIR}/lib" NO_DEFAULT_PATH)
if(NOT VIPS)
    # Cannot set CFLAGS with space separated flags
    ExternalProject_Add(libvips
      URL "https://github.com/libvips/libvips/archive/v${VIPS_VERSION}.tar.gz"
      PREFIX "${CMAKE_CURRENT_BINARY_DIR}/libvips"
      CONFIGURE_COMMAND ./autogen.sh
      PKG_CONFIG_PATH=${EXT_INSTALL_DIR}/lib/pkgconfig:$PKG_CONFIG_PATH
      LDFLAGS=-L${EXT_INSTALL_DIR}/lib,-Wl,-rpath=${EXT_INSTALL_DIR}/lib
      CPPFLAGS=-I${EXT_INSTALL_DIR}/include
      --prefix=${EXT_INSTALL_DIR}
      ${CONFIGURE_HOST}
      --enable-shared
      --disable-static
      --disable-dependency-tracking
      --with-libwebp
      --with-lcms
      --with-imagequant
      --with-jpeg
      --with-jpeg-includes=${EXT_INSTALL_DIR}/include
      --with-jpeg-libraries=${EXT_INSTALL_DIR}/lib
      --without-magick
      --without-orc
      --without-gsf
      --without-rsvg
      DEPENDS libjpeg libpng giflib libwebp libimagequant lcms2
      BUILD_IN_SOURCE 1
      )
else()
    add_custom_target(libvips "")
endif()

set(VIPS_INCLUDE_DIRS CACHE INTERNAL "${EXT_INSTALL_DIR}/include")
set(VIPS_LIBRARY_DIRS CACHE INTERNAL "${EXT_INSTALL_DIR}/lib")
set(VIPS_LIBRARIES CACHE INTERNAL "-lvips")
