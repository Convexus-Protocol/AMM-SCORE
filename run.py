#!/usr/bin/env python3

import argparse
from bash import bash

from scripts.config import Config
from scripts.deploy_contract import deploy, update
from scripts.call_contract import invoke, call
from scripts.meta import get_meta
from os.path import exists
import getpass

class Command:

    def __init__(self) -> None:
        parser = argparse.ArgumentParser()
        parser.add_argument('-e', '--endpoint', type=str, default='gochain', help='target endpoint for connection')
        parser.add_argument('-k', '--keystore', type=str, help='keystore path')
        parser.add_argument('-p', '--keystore_password', type=str, help='keystore password')

        subparsers = parser.add_subparsers(title='Available commands', dest='command')
        subparsers.required = True

        deploy_parser = subparsers.add_parser('deploy')
        deploy_parser.add_argument('package', type=str, help='package name')

        update_parser = subparsers.add_parser('update')
        update_parser.add_argument('package', type=str, help='package name')

        invoke_parser = subparsers.add_parser('invoke')
        invoke_parser.add_argument('package', type=str, help='package name')
        invoke_parser.add_argument('params', type=str, help='contract params filename')

        call_parser = subparsers.add_parser('call')
        call_parser.add_argument('package', type=str, help='package name')
        call_parser.add_argument('params', type=str, help='contract params filename')

        optimizedJar_parser = subparsers.add_parser('optimizedJar')
        optimizedJar_parser.add_argument('package', type=str, help='package name')

        args = parser.parse_args()
        getattr(self, args.command)(args)

    @staticmethod
    def get_keystore_path(keystore, network):
        if not keystore:
            return f"scripts/config/keystores/{network}/operator.icx"
        else:
            return keystore

    @staticmethod
    def get_keystore_password(password, keystore, network):
        if not password:
            if not keystore:
                keystore = f"scripts/config/keystores/{network}/operator.icx"

            target_path = keystore.replace('.icx', '.pwd')

            if exists(target_path):
                return open(target_path, "r").read()
            else:
                return getpass.getpass(prompt=f'Enter the keystore password ({keystore}): ')
        else:
            return password

    @staticmethod
    def optimizedJar(args):
        javaPkg, version, build = get_meta(args.package, args.endpoint)
        print(f"Executing {javaPkg}:build ...")
        result = str(bash(f"./gradlew {javaPkg}:build"))
        print(f"Executing {javaPkg}:optimizedJar ...")
        result = str(bash(f"./gradlew {javaPkg}:optimizedJar"))
        if not "BUILD SUCCESSFUL" in result:
            print(result)

    @staticmethod
    def deploy(args):
        print(f" -------------------- Deploying {args.package} ... -------------------- ")
        Command.optimizedJar(args)
        keystore_path = Command.get_keystore_path(args.keystore, args.endpoint)
        keystore_password = Command.get_keystore_password(args.keystore_password, args.keystore, args.endpoint)
        config = Config(args.endpoint, keystore_path, keystore_password)
        deploy(config, args.package)

    @staticmethod
    def update(args):
        print(f" -------------------- Updating {args.package} ... -------------------- ")
        Command.optimizedJar(args)
        keystore_path = Command.get_keystore_path(args.keystore, args.endpoint)
        keystore_password = Command.get_keystore_password(args.keystore_password, args.keystore, args.endpoint)
        config = Config(args.endpoint, keystore_path, keystore_password)
        update(config, args.package)

    @staticmethod
    def invoke(args):
        print(f" -------------------- Invoking {args.package} ... -------------------- ")
        keystore_path = Command.get_keystore_path(args.keystore, args.endpoint)
        keystore_password = Command.get_keystore_password(args.keystore_password, args.keystore, args.endpoint)
        config = Config(args.endpoint, keystore_path, keystore_password)
        invoke(config, args.package, args.params)

    @staticmethod
    def call(args):
        keystore_path = Command.get_keystore_path(args.keystore, args.endpoint)
        keystore_password = Command.get_keystore_password(args.keystore_password, args.keystore, args.endpoint)
        config = Config(args.endpoint, keystore_path, keystore_password)
        print(call(config, args.package, args.params))

if __name__ == "__main__":
    Command()
