#!/usr/bin/bash

#Set up the DuckUpdate.service on systemctl
mkdir -p /home/ec2-user/.config/systemd/user/
mv /home/ec2-user/DuckUpdate.service /home/ec2-user/.config/systemd/user/DuckUpdate.service

sudo loginctl enable-linger

systemctl --user daemon-reload
systemctl --user enable DuckUpdate.service
