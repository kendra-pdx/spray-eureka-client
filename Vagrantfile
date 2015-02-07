Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/trusty64"
  config.vm.provision "shell", path: "src/vagrant/provision_eureka.sh"
  config.vm.network "forwarded_port", guest: 8080, host: 7080
end