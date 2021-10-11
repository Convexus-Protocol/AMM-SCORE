#!/usr/bin/env python3

import argparse

from scripts.config import Config
from scripts.deploy_contract import deploy

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
        deploy_parser.add_argument('contract', type=str, help='target contract to deploy')
        deploy_parser.add_argument('version', type=str, help='target version to deploy')
        deploy_parser.add_argument('build', type=str, help='target build to deploy (debug, optimized)')
        deploy_parser.add_argument('params', type=str, help='deploy params path')

        args = parser.parse_args()
        getattr(self, args.command)(args)

    @staticmethod
    def deploy(args):
        config = Config(args.endpoint, args.keystore.name, args.password)
        deploy(config, args.contract, args.version, args.params, args.build, verbose=print)

if __name__ == "__main__":
    Command()
