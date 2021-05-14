FROM archlinux

RUN patched_glibc=glibc-linux4-2.33-4-x86_64.pkg.tar.zst && \
    curl -LO "https://repo.archlinuxcn.org/x86_64/$patched_glibc" && \
    bsdtar -C / -xvf "$patched_glibc"
RUN pacman --noconfirm -Syu --ignore glibc base-devel sudo

RUN pacman --noconfirm -S git openjdk-src
RUN pacman --noconfirm -S sbt

RUN sed -E 's/^#\s*(%wheel\s*ALL=\(ALL\)\s*ALL)/\1/' /etc/sudoers | EDITOR='tee' visudo
RUN useradd -Um -G wheel vscode &&\
    echo "vscode:vscode" | chpasswd

USER vscode
