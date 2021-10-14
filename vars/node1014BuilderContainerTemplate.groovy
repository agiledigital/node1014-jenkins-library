def call() {
	return [
		containerTemplate(
			name: 'node1014-builder',
			image: 'agiledigital/node1014-builder',
	    alwaysPullImage: true,
			command: 'cat',
			ttyEnabled: true,
			runAsUser: '1000',
			runAsGroup: '1000'
		)
	]
}