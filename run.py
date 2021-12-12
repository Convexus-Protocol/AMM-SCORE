#!/usr/bin/env python3

import argparse
from bash import bash

from scripts.config import Config
from scripts.deploy_contract import deploy, update
from scripts.call_contract import invoke
from scripts.meta import get_meta

class Command:

    def __init__(self) -> None:
        parser = argparse.ArgumentParser()
        parser.add_argument('-e', '--endpoint', type=str, default='gochain', help='target endpoint for connection')
        parser.add_argument('-k', '--keystore', type=argparse.FileType('r'), default='config/keystores/sejong/operator.icx',
                            help='keystore file for creating transactions')
        parser.add_argument('-p', '--password', type=str, default='gochain', help='keystore password')
        subparsers = parser.add_subparsers(title='Available commands', dest='command')
        subparsers.required = True

        deploy_parser = subparsers.add_parser('deploy')
        deploy_parser.add_argument('package', type=str, help='package name')

        update_parser = subparsers.add_parser('update')
        update_parser.add_argument('package', type=str, help='package name')

        invoke_parser = subparsers.add_parser('invoke')
        invoke_parser.add_argument('package', type=str, help='package name')
        invoke_parser.add_argument('params', type=str, help='contract params filename')

        optimizedJar_parser = subparsers.add_parser('optimizedJar')
        optimizedJar_parser.add_argument('package', type=str, help='package name')

        args = parser.parse_args()
        getattr(self, args.command)(args)

    @staticmethod
    def optimizedJar(args):
        javaPkg, version, build = get_meta(args.package, args.endpoint)
        print(f"Executing {javaPkg}:build...")
        result = str(bash(f"./gradlew {javaPkg}:build"))
        print(f"Executing {javaPkg}:optimizedJar...")
        result = str(bash(f"./gradlew {javaPkg}:optimizedJar"))
        if not "BUILD SUCCESSFUL" in result:
            print(result)

    @staticmethod
    def deploy(args):
        Command.optimizedJar(args)
        config = Config(args.endpoint, args.keystore.name, args.password)
        deploy(config, args.package, verbose=print)

    @staticmethod
    def update(args):
        Command.optimizedJar(args)
        config = Config(args.endpoint, args.keystore.name, args.password)
        update(config, args.package, verbose=print)

    @staticmethod
    def invoke(args):
        config = Config(args.endpoint, args.keystore.name, args.password)
        invoke(config, args.package, args.params, verbose=print)

if __name__ == "__main__":
    Command()
